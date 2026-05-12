import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import * as CodeMirror from 'codemirror';
import 'codemirror/mode/yaml/yaml';

@Component({
  selector: 'jhi-yaml-editor',
  template: '<textarea #ta class="jhi-yaml-editor-ta"></textarea>',
  styles: [
    `
      :host ::ng-deep .CodeMirror {
        border: 1px solid #ced4da;
        min-height: 420px;
        font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
        font-size: 13px;
      }
    `
  ]
})
export class YamlEditorComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('ta', { static: true }) ta!: ElementRef<HTMLTextAreaElement>;

  @Input() value = '';

  @Output() valueChange = new EventEmitter<string>();

  private cm: CodeMirror.Editor | undefined;

  ngAfterViewInit(): void {
    this.cm = CodeMirror.fromTextArea(this.ta.nativeElement, {
      lineNumbers: true,
      mode: 'yaml'
    });
    this.cm.setSize(null, '420px');
    this.cm.setValue(this.value ?? '');
    this.cm.on('change', () => {
      this.valueChange.emit(this.cm!.getValue());
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.cm && changes.value && !changes.value.firstChange) {
      const cur = this.cm.getValue();
      const next = changes.value.currentValue ?? '';
      if (cur !== next) {
        this.cm.setValue(next);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.cm) {
      const fromTextArea = this.cm as CodeMirror.Editor & { toTextArea(): void };
      const e = ((fromTextArea as unknown) as { getWrapperElement?: () => HTMLElement }).getWrapperElement?.();
      fromTextArea.toTextArea();
      if (e?.parentNode) {
        e.parentNode.removeChild(e);
      }
      this.cm = undefined;
    }
  }
}
