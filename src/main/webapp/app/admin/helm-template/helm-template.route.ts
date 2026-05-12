import { Route } from '@angular/router';
import { HelmTemplateEditorComponent } from './helm-template-editor.component';

export const helmTemplateRoute: Route = {
  path: '',
  component: HelmTemplateEditorComponent,
  data: {
    pageTitle: 'Helm templates (admin)'
  }
};
