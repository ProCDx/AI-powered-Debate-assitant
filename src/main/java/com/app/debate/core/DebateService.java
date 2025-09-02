package com.app.debate.core;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DebateService {

    // ------------------------------------------------------------------------
    // Simple data for topics (you can expand this or read from a file/db)
    // ------------------------------------------------------------------------
    private static final List<String> BASE_TOPICS = List.of(
            "Universal Basic Income",
            "Social Media Age Verification",
            "AI will take away jobs of humans",
            "School uniforms should be mandatory",
            "Ban single-use plastics"
    );

    public List<String> topics() {
        return BASE_TOPICS;
    }

    // ------------------------------------------------------------------------
    // PACK GENERATOR (existing feature)
    // ------------------------------------------------------------------------
    public Map<String, Object> generatePack(String topic, String stance) {
        String st = stance.equalsIgnoreCase("con") ? "CON" : "PRO";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("topic", topic);
        out.put("stance", st);

        // A tiny summary
        String summary = switch (topic.toLowerCase()) {
            case "universal basic income" -> """
                    Universal Basic Income (UBI) gives all citizens a regular, unconditional sum.
                    Supporters say it simplifies welfare and cushions automation shocks; critics worry about fiscal cost, inflation, and labor supply effects.
                    """;
            case "social media age verification" -> """
                    Mandatory age checks aim to protect minors from harms like grooming or addictive design.
                    Methods include ID, bank checks, or AI face estimation; privacy, security, and exclusion risks are key concerns.
                    """;
            case "ai will take away jobs of humans" -> """
                    AI can automate routine tasks and augment complex ones. Proponents expect higher productivity and new job creation;
                    opponents warn of net displacement, wage pressure, and skill polarization.
                    """;
            default -> "A public policy question with trade-offs in cost, freedom, risk, and fairness.";
        };
        out.put("summary", summary.trim());

        // Arguments (dummy examples)
        List<String> args = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        List<String> refs = new ArrayList<>();

        if (topic.toLowerCase().contains("basic income")) {
            args = List.of(
                    "Provides a floor against poverty and income shocks.",
                    "Simplifies welfare administration and reduces leakage.",
                    "Supports entrepreneurship and risk-taking.",
                    "Helps transition during automation-driven job losses."
            );
            cons = List.of(
                    "High fiscal cost; tax base may not sustain it.",
                    "Potential inflation or reduced labor supply.",
                    "May crowd out targeted, more efficient programs."
            );
            facts = List.of(
                    "Finland and small pilots suggest improved well-being.",
                    "Administrative simplicity can reduce overhead.",
                    "Entrepreneurship rates can rise with income security."
            );
            refs = List.of(
                    "https://en.wikipedia.org/wiki/Universal_basic_income",
                    "https://ourworldindata.org/income-inequality",
                    "https://www.imf.org"
            );
        } else if (topic.toLowerCase().contains("age verification")) {
            args = List.of(
                    "Better protection of minors from explicit content.",
                    "Improves accountability and reporting mechanisms.",
                    "Enables safer design defaults for minors."
            );
            cons = List.of(
                    "Large privacy and surveillance risks.",
                    "Excludes users without IDs or bank accounts.",
                    "Creates attractive targets for identity theft."
            );
            facts = List.of(
                    "AI estimation can reduce friction but may be biased.",
                    "Breach risks rise when verifying identity at scale."
            );
            refs = List.of(
                    "https://ico.org.uk/for-organisations/childrens-code-hub/",
                    "https://oag.ca.gov/privacy/ccpa"
            );
        } else {
            args = List.of(
                    "Potential benefits outweigh manageable risks.",
                    "Global trend suggests feasibility with proper guardrails."
            );
            cons = List.of(
                    "Costs and externalities are under-estimated.",
                    "Implementation risks could backfire without oversight."
            );
            facts = List.of(
                    "Comparative studies show mixed outcomes.",
                    "Stronger institutions improve results."
            );
            refs = List.of("https://en.wikipedia.org", "https://ourworldindata.org");
        }

        out.put("arguments", stance.equalsIgnoreCase("con") ? cons : args);
        out.put("counter_arguments", stance.equalsIgnoreCase("con") ? args : cons);
        out.put("facts", facts);
        out.put("references", refs);
        return out;
    }

    // ------------------------------------------------------------------------
    // SOLO SPARRING (heuristic counter generator; safe offline fallback)
    // ------------------------------------------------------------------------

    public Map<String, Object> sparCounter(
            String topic,
            String side,          // user's side (pro/con)
            String difficulty,    // basic|advanced
            String mode,          // free|cwi
            String userText,      // free text of user's last turn
            Map<String, String> cwi, // Claim/Warrant/Impact (optional)
            List<Map<String, Object>> history // last few turns
    ) {
        // Opponent side
        String opp = side.equals("con") ? "pro" : "con";

        // Build a concise, structured counter (heuristic)
        String counterText = generateHeuristicCounter(topic, opp, userText, cwi, difficulty);

        Map<String, Object> counterObj = new LinkedHashMap<>();
        counterObj.put("text", counterText);

        // If mode=cwi, try to map to C/W/I
        if ("cwi".equals(mode)) {
            Map<String, String> cwiBot = new LinkedHashMap<>();
            cwiBot.put("claim", extractClaim(counterText));
            cwiBot.put("warrant", extractWarrant(counterText));
            cwiBot.put("impact", extractImpact(counterText));
            counterObj.put("cwi", cwiBot);
        }

        List<String> labels = new ArrayList<>();
        if (userText.toLowerCase().contains("cost")) labels.add("weighing:magnitude");
        if (userText.toLowerCase().contains("privacy")) labels.add("weighing:reversibility");
        if (userText.toLowerCase().contains("jobs")) labels.add("weighing:probability");
        counterObj.put("labels", labels);

        String hint = makeHint(userText, cwi);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counter", counterObj);
        out.put("hint", hint);
        return out;
    }

    // Heuristic counter-builder: short, targeted response templates
    private String generateHeuristicCounter(
            String topic, String oppSide, String userText, Map<String, String> cwi, String difficulty
    ) {
        String base = switch (oppSide) {
            case "pro" -> "Even if risks exist, %s delivers higher net benefits with proper guardrails.";
            default -> "Even if intended benefits exist, %s carries outsized risks and trade-offs.";
        };

        String spine = String.format(base, topic);

        // Keyword nudges
        String extra = "";
        String ut = (userText == null ? "" : userText).toLowerCase();

        if (ut.contains("cost") || ut.contains("tax"))
            extra = oppSide.equals("con")
                    ? " Funding requirements scale poorly; opportunity cost crowds out targeted programs."
                    : " Aggregate fiscal capacity can support phased pilots; targeted offsets limit inflationary pressure.";
        else if (ut.contains("privacy") || ut.contains("surveillance"))
            extra = oppSide.equals("con")
                    ? " Centralized identity checks create honey-pot risks and normalization of surveillance."
                    : " Privacy-preserving verification (zero-knowledge proofs, on-device checks) mitigates exposure.";
        else if (ut.contains("jobs") || ut.contains("automation"))
            extra = oppSide.equals("con")
                    ? " Displacement is persistent; reskilling programs have mixed evidence at scale."
                    : " Transition support plus productivity gains raise new sectors and net employment over time.";
        else if (ut.contains("inflation"))
            extra = oppSide.equals("con")
                    ? " Broad transfers elevate demand faster than supply; risks of persistent price pressure."
                    : " Sequenced transfers plus supply-side policy and targeting keep inflation anchored.";
        else if (ut.contains("fair") || ut.contains("equity") || ut.contains("inequality"))
            extra = oppSide.equals("con")
                    ? " Uniform benefits can misallocate resources and neglect those with highest need."
                    : " Universality reduces stigma and leakage; equity improves through floor effects.";
        else
            extra = oppSide.equals("con")
                    ? " Implementation risks, fiscal exposures, and negative externalities are under-priced."
                    : " Proper governance, staged rollout, and oversight keep risks manageable.";

        if ("advanced".equals(difficulty)) {
            extra += " Weighing: magnitude and probability favor this side under conservative assumptions.";
        }

        // If structured input exists, refute warrant specifically
        if (cwi != null) {
            String claim = safe(cwi.get("claim"));
            String warrant = safe(cwi.get("warrant"));
            String impact = safe(cwi.get("impact"));
            if (!warrant.isEmpty()) {
                extra += " Your warrant (“" + warrant + "”) is not generalizable; counter-evidence suggests limits.";
            }
            if (!impact.isEmpty()) {
                extra += " Even if impact (“" + impact + "”) occurs, its magnitude is likely lower than alternatives.";
            }
            if (!claim.isEmpty()) {
                extra += " A more defensible claim would bound conditions rather than assert a universal rule.";
            }
        }
        return (spine + " " + extra).trim();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String extractClaim(String text) {
        return "Opposes the core link while proposing manageable alternatives.";
    }

    private static String extractWarrant(String text) {
        return "Risks can be bounded with governance and staged rollout (or are larger than stated).";
    }

    private static String extractImpact(String text) {
        return "Net societal outcomes favor this side under conservative assumptions.";
    }

    private static String makeHint(String userText, Map<String, String> cwi) {
        if (cwi != null && safe(cwi.get("warrant")).isEmpty()) {
            return "Try adding a clearer Warrant that links your Claim to the Impact with mechanism and evidence.";
        }
        if (userText != null && userText.length() < 40) {
            return "Expand your turn to 1–2 sentences. Address counter-examples and add weighing.";
        }
        return "Consider explicit weighing (magnitude, probability, timeframe, reversibility).";
    }
}
