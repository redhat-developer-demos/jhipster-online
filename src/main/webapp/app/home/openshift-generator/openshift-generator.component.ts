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
  standalone: false,
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
    hideDatabaseType: false,
    hideProdDatabaseTypeOptions: ['mysql', 'oracle', 'mssql', 'cassandra', 'couchbase', 'neo4j', 'no'],
    hideDevDatabaseTypeOptions: ['h2Disk', 'postgresql', 'mysql', 'mariadb', 'oracle', 'mssql', 'cassandra', 'couchbase', 'neo4j', 'no'],
    hideCacheProvider: true,
    hideBuildTool: false,
    hideOtherComponents: false,
    hideClientSideOptions: false,
    hideI18nOptions: true,
    hideTestingOptions: true,
    hideBackendFramework: false
  };
  openshiftJHipsterModel: JHipsterConfigurationModel = new JHipsterConfigurationModel();

  namespace = '';
  namespaces: string[] = [];
  deployToCluster = false;
  deployStatus = '';
  deployMethod: 'fabric8' | 'argocd' = 'fabric8';
  argocdApplicationNamespace = 'openshift-gitops';

  scaffoldApps: OpenshiftScaffoldRow[] = [];

  scaffoldLoadError = '';

  /** Shown when Helm CLI failed but Fabric8 fallback succeeded. */
  deployHelmWarning = '';
  deployRhbk = false;
  rhbkAdminPassword = 'changeme';

  constructor(private http: HttpClient) {}

  /** Passed to generator log modal — runs after async push + generation succeed. */
  generationFinishedHandler = (): void => {
    this.loadScaffoldApps();
    if (this.deployToCluster && this.namespace?.trim()) {
      // Run after the current turn so generator form state is stable, then clone+apply from Git.
      setTimeout(() => this.onDeployToCluster(), 0);
    }
  };

  ngOnInit(): void {
    this.openshiftJHipsterModel.backendFramework = 'quarkus';
    this.openshiftJHipsterModel.clientFramework = 'vue';
    this.openshiftJHipsterModel.cacheProvider = 'no';
    this.openshiftJHipsterModel.enableHibernateCache = false;
    this.openshiftJHipsterModel.websocket = false;
    this.openshiftJHipsterModel.enableSwaggerCodegen = false;
    this.openshiftJHipsterModel.blueprints = [{ name: 'generator-jhipster-quarkus' }];
    this.openshiftJHipsterModel.devDatabaseType = 'h2Memory';
    this.openshiftJHipsterModel.prodDatabaseType = 'mariadb';
    this.openshiftJHipsterModel.withAdminUi = true;
    this.loadNamespaces();
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
    const provider = (this.generatorRef?.selectedGitProvider || '').toLowerCase();
    const gitOwner = this.generatorRef?.selectedGitCompany || '';
    this.deployToOpenShift(gitRepo, appName, provider, gitOwner, appName);
  }

  deployFromScaffold(row: OpenshiftScaffoldRow): void {
    if (!this.namespace?.trim()) {
      this.deployStatus = 'Select a target namespace first.';
      return;
    }
    if (!this.deployToCluster) {
      this.deployToCluster = true;
    }
    this.deployToOpenShift(row.gitRepoUrl, row.repositoryName, row.gitProvider, row.gitCompany, row.repositoryName);
  }

  removeFromScaffoldList(row: OpenshiftScaffoldRow): void {
    const hasNamespace = !!this.namespace?.trim();
    const msg = hasNamespace
      ? 'Remove "' +
        row.repositoryName +
        '" from your deploy list AND undeploy from namespace ' +
        this.namespace +
        '?\n\n(Cancel to keep the cluster resources; the Git repo is never deleted.)'
      : 'Remove this repository from your deploy list? (The Git repo is not deleted.)';

    if (!window.confirm(msg)) {
      return;
    }

    if (hasNamespace) {
      this.http.delete('api/openshift/applications/' + row.repositoryName + '?namespace=' + this.namespace).subscribe(
        () => {
          this.deployStatus = 'Undeployed ' + row.repositoryName + ' from ' + this.namespace;
        },
        () => {
          this.deployStatus = 'Could not undeploy from cluster (may not exist). List entry removed.';
        }
      );
    }

    this.http.delete('api/openshift-scaffold-applications/' + row.id).subscribe(
      () => this.loadScaffoldApps(),
      () => {
        this.deployStatus = 'Could not remove list entry.';
      }
    );
  }

  private deployToOpenShift(gitRepo: string, appName: string, gitProvider?: string, gitOwner?: string, gitRepoName?: string): void {
    if (!this.namespace?.trim()) {
      return;
    }
    this.deployStatus = 'Deploying to namespace ' + this.namespace + ' (' + this.deployMethod + ')...';
    this.deployHelmWarning = '';
    const body: any = {
      namespace: this.namespace,
      gitRepo,
      appName,
      deployMethod: this.deployMethod,
      argocdApplicationNamespace: this.argocdApplicationNamespace
    };
    if (gitProvider) {
      body.gitProvider = gitProvider;
    }
    if (gitOwner) {
      body.gitOwner = gitOwner;
    }
    if (gitRepoName) {
      body.gitRepoName = gitRepoName;
    }
    const auth = this.generatorRef?.model?.authenticationType;
    if (this.deployMethod === 'fabric8' && this.deployRhbk && auth === 'oauth2') {
      body.deployRhbk = true;
      body.rhbkAdminPassword = (this.rhbkAdminPassword || 'changeme').trim();
    }
    this.http.post<any>('api/openshift/deploy', body).subscribe(
      (result: any) => {
        this.deployHelmWarning = typeof result?.helmWarning === 'string' ? result.helmWarning : '';
        if (this.deployMethod === 'argocd') {
          this.deployStatus =
            'Argo CD Application ' +
            (result.application || appName) +
            ' applied in ' +
            (result.argocdNamespace || this.argocdApplicationNamespace);
        } else {
          let msg =
            result.deployMethod === 'helm' && result.release
              ? 'Helm release ' + result.release + ' installed in ' + this.namespace
              : 'Deployed ' + (result.resources?.length || 0) + ' resources to ' + this.namespace;
          if (result.webhookUrl) {
            msg += ' | Webhook configured: ' + result.webhookUrl;
          }
          this.deployStatus = msg;
        }
        this.loadScaffoldApps();
      },
      (error: any) => {
        this.deployStatus = 'Deploy failed: ' + (error.error?.error || error.message);
      }
    );
  }
}
