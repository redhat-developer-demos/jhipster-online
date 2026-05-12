package io.github.jhipster.online.web.rest.vm;

public class EditorAiTextResponseVM {

    private String text;

    public EditorAiTextResponseVM() {}

    public EditorAiTextResponseVM(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
