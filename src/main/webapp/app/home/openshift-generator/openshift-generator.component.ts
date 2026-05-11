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

import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GeneratorComponent } from '../generator/generator.component';
import { GeneratorConfigurationModel } from '../generator/generator.configuration.model';
import { JHipsterConfigurationModel } from '../generator/jhipster.configuration.model';

export interface OpenshiftScaffoldRow {
  id: number;
  gitRepoUrl: string;
  repositoryName: string;
  gitCompany: string;
  gitProvider: string;
  framework: string;
  createdDate: string;
}

@Component({
  selector: 'jhi-openshift-generator',
  templateUrl: './openshift-generator.component.html'
})
export class OpenshiftGeneratorComponent implements OnInit {
  @ViewChild('generatorRef') generatorRef?: GeneratorComponent;

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
  deployMethod: 'fabric8' | 'argocd' = 'fabric8';
  argocdApplicationNamespace = 'openshift-gitops';

  kubernetesExtrasYaml = '';

  snippetPresets: { id: string; title: string }[] = [];

  snippetLoadError = '';

  scaffoldApps: OpenshiftScaffoldRow[] = [];

  scaffoldLoadError = '';

  constructor(private http: HttpClient) {}

  /** Passed to generator log modal — runs after async push + generation succeed. */
  generationFinishedHandler = (): void => {
    this.loadScaffoldApps();
  };

  ngOnInit(): void {
    this.openshiftJHipsterModel.devDatabaseType = 'h2Memory';
    this.openshiftJHipsterModel.prodDatabaseType = 'mariadb';
    this.openshiftJHipsterModel.cacheProvider = 'no';
    this.openshiftJHipsterModel.clientFramework = 'vue';
    this.openshiftJHipsterModel.withAdminUi = true;
    this.loadNamespaces();
    this.loadSnippetPresets();
    this.loadScaffoldApps();
  }

  loadScaffoldApps(): void {
    this.http.get<OpenshiftScaffoldRow[]>('api/openshift-scaffold-applications').subscribe(
      data => {
        this.scaffoldApps = data ?? [];
        this.scaffoldLoadError = '';
      },
      () => {
        this.scaffoldApps = [];
        this.scaffoldLoadError = 'Could not load your OpenShift application list.';
      }
    );
  }

  loadSnippetPresets(): void {
    this.http.get<{ id: string; title: string }[]>('api/kubernetes-snippets').subscribe(
      data => {
        this.snippetPresets = data ?? [];
        this.snippetLoadError = '';
      },
      () => {
        this.snippetPresets = [];
        this.snippetLoadError = 'Could not load Kubernetes snippet presets.';
      }
    );
  }

  appendPresetToKubernetesExtras(presetId: string): void {
    this.http.get('api/kubernetes-snippets/' + encodeURIComponent(presetId), { responseType: 'text' }).subscribe(
      text => {
        const sep = this.kubernetesExtrasYaml.trim().length > 0 ? '\n---\n' : '';
        this.kubernetesExtrasYaml = (this.kubernetesExtrasYaml + sep + text).replace(/^\s+/, '');
      },
      () => {
        this.snippetLoadError = 'Failed to load preset: ' + presetId;
      }
    );
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

  private resolveGitRepo(): string {
    const g = this.generatorRef;
    if (!g?.selectedGitCompany || !g.repositoryName) {
      return '';
    }
    const provider = (g.selectedGitProvider || '').toLowerCase();
    if (provider.includes('gitlab')) {
      return `https://gitlab.com/${g.selectedGitCompany}/${g.repositoryName}`;
    }
    if (provider.includes('gitea')) {
      const host = (g.gitConfig?.giteaHost || 'https://gitea.com').replace(/\/+$/, '');
      return `${host}/${g.selectedGitCompany}/${g.repositoryName}`;
    }
    return `https://github.com/${g.selectedGitCompany}/${g.repositoryName}`;
  }

  onDeployToCluster(): void {
    if (!this.deployToCluster || !this.namespace) {
      return;
    }
    const gitRepo = this.resolveGitRepo();
    const appName = this.generatorRef?.repositoryName?.trim();
    if (!gitRepo || !appName) {
      this.deployStatus =
        'Configure Git provider, organization/group, and repository name above, then push your generated repo before deploying.';
      return;
    }
    this.deployToOpenShift(gitRepo, appName);
  }

  deployFromScaffold(row: OpenshiftScaffoldRow): void {
    if (!this.namespace?.trim()) {
      this.deployStatus = 'Select a target namespace first.';
      return;
    }
    if (!this.deployToCluster) {
      this.deployToCluster = true;
    }
    this.deployToOpenShift(row.gitRepoUrl, row.repositoryName);
  }

  removeFromScaffoldList(row: OpenshiftScaffoldRow): void {
    if (!window.confirm('Remove this repository from your deploy list? (The Git repo is not deleted.)')) {
      return;
    }
    this.http.delete('api/openshift-scaffold-applications/' + row.id).subscribe(
      () => this.loadScaffoldApps(),
      () => {
        this.deployStatus = 'Could not remove list entry.';
      }
    );
  }

  private deployToOpenShift(gitRepo: string, appName: string): void {
    if (!this.namespace?.trim()) {
      return;
    }
    this.deployStatus = 'Deploying to namespace ' + this.namespace + ' (' + this.deployMethod + ')...';
    this.http
      .post<any>('api/openshift/deploy', {
        namespace: this.namespace,
        gitRepo,
        appName,
        deployMethod: this.deployMethod,
        argocdApplicationNamespace: this.argocdApplicationNamespace
      })
      .subscribe(
        (result: any) => {
          if (this.deployMethod === 'argocd') {
            this.deployStatus =
              'Argo CD Application ' +
              (result.application || appName) +
              ' applied in ' +
              (result.argocdNamespace || this.argocdApplicationNamespace);
          } else {
            this.deployStatus = 'Deployed ' + (result.resources?.length || 0) + ' resources to ' + this.namespace;
          }
          this.loadScaffoldApps();
        },
        (error: any) => {
          this.deployStatus = 'Deploy failed: ' + (error.error?.error || error.message);
        }
      );
  }
}
