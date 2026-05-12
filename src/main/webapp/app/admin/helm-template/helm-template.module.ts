import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { JhonlineSharedModule } from 'app/shared/shared.module';
import { HelmTemplateEditorComponent } from './helm-template-editor.component';
import { YamlEditorComponent } from './yaml-editor.component';
import { helmTemplateRoute } from './helm-template.route';

@NgModule({
  imports: [JhonlineSharedModule, FormsModule, RouterModule.forChild([helmTemplateRoute])],
  declarations: [HelmTemplateEditorComponent, YamlEditorComponent]
})
export class HelmTemplateModule {}
