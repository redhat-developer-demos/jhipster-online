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

import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GeneratorConfigurationModel } from '../generator/generator.configuration.model';
import { JHipsterConfigurationModel } from '../generator/jhipster.configuration.model';

@Component({
  selector: 'jhi-openshift-generator',
  templateUrl: './openshift-generator.component.html'
})
export class OpenshiftGeneratorComponent implements OnInit {
  openshiftGeneratorConfig: GeneratorConfigurationModel = {
    hideRepositoryName: false,
    hideApplicationType: false,
    hideServiceDiscoveryType: true,
    hideAuthenticationType: false,
    hideDatabaseType: true,
    hideProdDatabaseTypeOptions: ['mysql', 'postgresql', 'oracle', 'mssql', 'mongodb', 'cassandra', 'couchbase', 'neo4j', 'no'],
    hideDevDatabaseTypeOptions: [
      'h2Disk',
      'postgresql',
      'mysql',
      'mariadb',
      'oracle',
      'mssql',
      'mongodb',
      'cassandra',
      'couchbase',
      'neo4j',
      'no'
    ],
    hideCacheProvider: true,
    hideBuildTool: false,
    hideOtherComponents: false,
    hideClientSideOptions: false,
    hideI18nOptions: true,
    hideTestingOptions: true
  };
  openshiftJHipsterModel: JHipsterConfigurationModel = new JHipsterConfigurationModel();

  namespace = '';
  namespaces: string[] = [];
  deployToCluster = false;
  deployStatus = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.openshiftJHipsterModel.devDatabaseType = 'h2Memory';
    this.openshiftJHipsterModel.prodDatabaseType = 'mariadb';
    this.openshiftJHipsterModel.cacheProvider = 'no';
    this.openshiftJHipsterModel.clientFramework = 'vue';
    this.openshiftJHipsterModel.withAdminUi = true;
    this.loadNamespaces();
  }

  loadNamespaces(): void {
    this.http.get<string[]>('api/openshift/namespaces').subscribe(
      (data: string[]) => {
        this.namespaces = data;
        if (data.length > 0 && !this.namespace) {
          this.namespace = data[0];
        }
      },
      () => {
        this.namespaces = [];
      }
    );
  }

  onDeployToCluster(): void {
    if (!this.deployToCluster || !this.namespace) {
      return;
    }
    this.deployStatus = 'Deploying to namespace ' + this.namespace + '...';
    this.http
      .post<any>('api/openshift/deploy', {
        namespace: this.namespace,
        templateUrl:
          'https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/template.yaml',
        NAMESPACE: this.namespace
      })
      .subscribe(
        (result: any) => {
          this.deployStatus = 'Deployed ' + result.resourceCount + ' resources to ' + this.namespace;
        },
        (error: any) => {
          this.deployStatus = 'Deploy failed: ' + (error.error?.error || error.message);
        }
      );
  }
}
