package io.github.jhipster.online.web.rest.vm;

/**
 * Returned when {@code POST /api/jdl-ai/generate} fails after contacting the model (HTTP 502).
 */
public class JdlAiErrorVM {

    private String detail;

    public JdlAiErrorVM() {}

    public JdlAiErrorVM(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
