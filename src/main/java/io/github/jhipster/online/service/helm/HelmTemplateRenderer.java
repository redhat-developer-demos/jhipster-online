package io.github.jhipster.online.service.helm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Helm-style rendering for {@code {{ .Values.* }}} placeholders (no Sprig, no include).
 * Used by {@link io.github.jhipster.online.service.OpenShiftDeploymentService} for Fabric8 direct apply.
 */
public final class HelmTemplateRenderer {

    private static final Pattern VALUES_PATTERN = Pattern.compile("\\{\\{-?\\s*\\.Values\\.([\\w.\\-]+)\\s*-?\\}\\}");

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
        Matcher m = VALUES_PATTERN.matcher(template);
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
}
