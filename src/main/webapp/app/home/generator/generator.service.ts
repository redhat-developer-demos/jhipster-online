/**
 * Copyright 2017-2024 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { JHipsterConfigurationModel } from './jhipster.configuration.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class GeneratorService {
  constructor(private http: HttpClient) {}

  download(jhipsterConfigurationModel: JHipsterConfigurationModel): Observable<HttpResponse<Blob>> {
    return this.http.post(
      'api/download-application',
      { 'generator-jhipster': jhipsterConfigurationModel },
      { observe: 'response', responseType: 'blob' }
    );
  }

  generateOnGit(
    jhipsterConfigurationModel: JHipsterConfigurationModel,
    gitProvider: string,
    gitCompany: string,
    repositoryName: string
  ): Observable<string> {
    return this.http.post(
      'api/generate-application',
      {
        'generator-jhipster': jhipsterConfigurationModel,
        'git-provider': gitProvider,
        'git-company': gitCompany,
        'repository-name': repositoryName
      },
      { responseType: 'text' }
    );
  }

  getGenerationData(applicationId: string): Observable<string> {
    return this.http.get('api/generate-application/' + applicationId, { responseType: 'text' });
  }

  getNamespaces(): Observable<string[]> {
    return this.http.get<string[]>('api/openshift/namespaces');
  }

  deployToOpenShift(namespace: string, templateUrl: string, params: any): Observable<any> {
    return this.http.post('api/openshift/deploy', { namespace, templateUrl, ...params });
  }

  triggerPipeline(namespace: string, gitRepo: string, appName: string, appJarVersion: string): Observable<any> {
    return this.http.post('api/openshift/pipeline', { namespace, gitRepo, appName, appJarVersion });
  }

  getDeployedApplications(namespace: string): Observable<any[]> {
    return this.http.get<any[]>('api/openshift/applications?namespace=' + namespace);
  }

  deleteDeployedApplication(namespace: string, name: string): Observable<void> {
    return this.http.delete<void>('api/openshift/applications/' + name + '?namespace=' + namespace);
  }

  checkPermissions(namespace: string): Observable<any> {
    return this.http.get('api/openshift/permissions?namespace=' + namespace);
  }
}
