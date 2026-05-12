package io.github.jhipster.online.service.helm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Helm-style rendering for {@code {{ .Values.* }}} placeholders plus
 * {@code {{- if ... }}...{{- end }}} conditional blocks.
 * Used by {@link io.github.jhipster.online.service.OpenShiftDeploymentService} for Fabric8 direct apply.
 */
public final class HelmTemplateRenderer {

    private static final Pattern VALUES_PATTERN = Pattern.compile("\\{\\{-?\\s*\\.Values\\.([\\w.\\-]+)\\s*-?\\}\\}");

    private static final Pattern IF_BLOCK = Pattern.compile(
        "^[^\\S\\n]*\\{\\{-?\\s*if\\s+(.+?)\\s*-?\\}\\}\\s*?\\n?(.*?)\\{\\{-?\\s*end\\s*-?\\}\\}[^\\S\\n]*\\n?",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern LEFTOVER_BRACES = Pattern.compile("^[^\\S\\n]*\\{\\{.*?\\}\\}[^\\S\\n]*\\n?", Pattern.MULTILINE);
    private static final Pattern LEFTOVER_INLINE_BRACES = Pattern.compile("\\{\\{.*?\\}\\}");

    private HelmTemplateRenderer() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> flattenValues(Map<String, Object> nested) {
        Map<String, Object> flat = new LinkedHashMap<>();
        flatten("", nested, flat);
        return flat;
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> map, Map<String, Object> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Map) {
                flatten(key, (Map<String, Object>) v, out);
            } else {
                out.put(key, v);
            }
        }
    }

    public static String render(String template, Map<String, Object> flatValues) {
        // Normalize accidental spaces inside Helm delimiters (e.g. "{ { .Values.x } }") so .Values substitution works.
        String normalized = template.replace("{ {", "{{").replace("} }", "}}");
        String result = resolveIfBlocks(normalized, flatValues);
        result = resolveValues(result, flatValues);
        result = stripLeftoverDirectives(result);
        return result;
    }

    private static String resolveValues(String text, Map<String, Object> flatValues) {
        Matcher m = VALUES_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String path = m.group(1);
            Object val = flatValues.get(path);
            String replacement = val != null ? Matcher.quoteReplacement(String.valueOf(val)) : "";
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveIfBlocks(String text, Map<String, Object> flatValues) {
        String prev;
        String current = text;
        do {
            prev = current;
            Matcher m = IF_BLOCK.matcher(current);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String condition = m.group(1).trim();
                String body = m.group(2);
                boolean keep = evaluateCondition(condition, flatValues);
                m.appendReplacement(sb, keep ? Matcher.quoteReplacement(body) : "");
            }
            m.appendTail(sb);
            current = sb.toString();
        } while (!current.equals(prev));
        return current;
    }

    private static boolean evaluateCondition(String condition, Map<String, Object> flatValues) {
        if (condition.startsWith("and ")) {
            String rest = condition.substring(4).trim();
            String[] parts = splitConditionArgs(rest);
            for (String part : parts) {
                if (!evaluateCondition(part.trim(), flatValues)) {
                    return false;
                }
            }
            return true;
        }
        if (condition.startsWith("(") && condition.endsWith(")")) {
            return evaluateCondition(condition.substring(1, condition.length() - 1).trim(), flatValues);
        }
        Matcher neMatcher = Pattern.compile("ne\\s+\\.Values\\.([\\w.\\-]+)\\s+\"(.*)\"").matcher(condition);
        if (neMatcher.matches()) {
            Object v = flatValues.get(neMatcher.group(1));
            return v != null && !String.valueOf(v).equals(neMatcher.group(2));
        }
        Matcher eqMatcher = Pattern.compile("eq\\s+\\.Values\\.([\\w.\\-]+)\\s+\"(.*)\"").matcher(condition);
        if (eqMatcher.matches()) {
            Object v = flatValues.get(eqMatcher.group(1));
            return v != null && String.valueOf(v).equals(eqMatcher.group(2));
        }
        if (condition.startsWith(".Values.")) {
            String path = condition.substring(8);
            return isTruthy(flatValues.get(path));
        }
        return false;
    }

    private static String[] splitConditionArgs(String s) {
        int depth = 0;
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == ' ' && depth == 0 && current.length() > 0) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        String s = String.valueOf(val).trim();
        return !s.isEmpty() && !s.equals("false") && !s.equals("0");
    }

    private static String stripLeftoverDirectives(String text) {
        String result = LEFTOVER_BRACES.matcher(text).replaceAll("");
        return LEFTOVER_INLINE_BRACES.matcher(result).replaceAll("");
    }
}
