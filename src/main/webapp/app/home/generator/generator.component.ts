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
import { Component, Input, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { GitConfigurationModel } from 'app/core/git/git-configuration.model';
import { GitConfigurationService } from 'app/core/git/git-configuration.service';
import { AccountService } from 'app/core/auth/account.service';

import { JHipsterConfigurationModel, BlueprintModel } from './jhipster.configuration.model';
import { GeneratorService } from './generator.service';
import { GeneratorOutputDialogComponent } from './generator.output.component';
import {
  AllDevDatabaseTypes,
  AllProdDatabaseTypes,
  DatabaseType,
  DevDatabaseType,
  GeneratorConfigurationModel,
  ProdDatabaseType
} from './generator.configuration.model';

@Component({
  standalone: false,
  selector: 'jhi-generator',
  templateUrl: './generator.component.html'
})
export class GeneratorComponent implements OnInit {
  @Input() config: GeneratorConfigurationModel = {};
  @Input() defaultModel: Partial<JHipsterConfigurationModel> | undefined;
  /** Optional YAML merged into generated `src/main/kubernetes/jh-online-kubernetes-extras.yaml` (OpenShift flow). */
  @Input() kubernetesExtrasYaml = '';

  /** When true, server stores this Git repo in the OpenShift deploy list after generation. */
  @Input() openshiftGeneratorApplication = false;

  /** Invoked when async Git generation completes successfully (log modal sees "Generation finished"). */
  @Input() generationFinishedCallback?: () => void;

  model: JHipsterConfigurationModel = new JHipsterConfigurationModel();

  submitted = false;

  languageOptions: any;
  iaCToolOptions: any;

  selectedGitProvider: string | undefined;
  selectedGitCompany: string | undefined;

  githubConfigured = false;
  gitlabConfigured = false;
  giteaConfigured = false;

  repositoryName: string | undefined;

  gitConfig: GitConfigurationModel | undefined;

  isStatsEnabled = false;

  constructor(
    private modalService: NgbModal,
    private generatorService: GeneratorService,
    private gitConfigurationService: GitConfigurationService,
    private accountService: AccountService
  ) {}

  /**
   * get all the languages options supported by JHipster - copied from the generator.
   */
  static getAllSupportedLanguageOptions(): any {
    return [
      { name: 'Albanian', value: 'al' },
      { name: 'Arabic (Libya)', value: 'ar-ly' },
      { name: 'Armenian', value: 'hy' },
      { name: 'Belorussian', value: 'by' },
      { name: 'Bengali', value: 'bn' },
      { name: 'Catalan', value: 'ca' },
      { name: 'Chinese (Simplified)', value: 'zh-cn' },
      { name: 'Chinese (Traditional)', value: 'zh-tw' },
      { name: 'Czech', value: 'cs' },
      { name: 'Danish', value: 'da' },
      { name: 'Dutch', value: 'nl' },
      { name: 'English', value: 'en' },
      { name: 'Estonian', value: 'et' },
      { name: 'Farsi', value: 'fa' },
      { name: 'Finnish', value: 'fi' },
      { name: 'French', value: 'fr' },
      { name: 'Galician', value: 'gl' },
      { name: 'German', value: 'de' },
      { name: 'Greek', value: 'el' },
      { name: 'Hindi', value: 'hi' },
      { name: 'Hungarian', value: 'hu' },
      { name: 'Indonesia', value: 'in' },
      { name: 'Italian', value: 'it' },
      { name: 'Japanese', value: 'ja' },
      { name: 'Korean', value: 'ko' },
      { name: 'Marathi', value: 'mr' },
      { name: 'Myanmar', value: 'my' },
      { name: 'Polish', value: 'pl' },
      { name: 'Portuguese (Brazilian)', value: 'pt-br' },
      { name: 'Portuguese', value: 'pt-pt' },
      { name: 'Romanian', value: 'ro' },
      { name: 'Russian', value: 'ru' },
      { name: 'Slovak', value: 'sk' },
      { name: 'Serbian', value: 'sr' },
      { name: 'Spanish', value: 'es' },
      { name: 'Swedish', value: 'sv' },
      { name: 'Turkish', value: 'tr' },
      { name: 'Tamil', value: 'ta' },
      { name: 'Telugu', value: 'te' },
      { name: 'Thai', value: 'th' },
      { name: 'Ukrainian', value: 'ua' },
      { name: 'Uzbek (Cyrillic)', value: 'uz-Cyrl-uz' },
      { name: 'Uzbek (Latin)', value: 'uz-Latn-uz' },
      { name: 'Vietnamese', value: 'vi' }
    ];
  }

  ngOnInit(): void {
    this.newGenerator();
    this.languageOptions = GeneratorComponent.getAllSupportedLanguageOptions();
    this.iaCToolOptions = [
      { name: 'Terraform', value: 'terraform' },
      { name: 'Bicep', value: 'bicep' }
    ];
    this.gitConfig = this.gitConfigurationService.gitConfig;
    if (this.gitConfig) {
      this.gitlabConfigured = this.gitConfig.gitlabConfigured ?? false;
      this.githubConfigured = this.gitConfig.githubConfigured ?? false;
      this.giteaConfigured = this.gitConfig.giteaConfigured ?? false;
    }
    this.gitConfigurationService.sharedData.subscribe((gitConfig: GitConfigurationModel) => {
      if (gitConfig) {
        this.gitlabConfigured = gitConfig.gitlabConfigured ?? false;
        this.githubConfigured = gitConfig.githubConfigured ?? false;
        this.giteaConfigured = gitConfig.giteaConfigured ?? false;
      }
    });
  }

  updateSharedData(data: any): void {
    this.selectedGitProvider = data.selectedGitProvider;
    this.selectedGitCompany = data.selectedGitCompany;
  }

  checkModelBeforeSubmit(): void {
    this.submitted = true;

    if (this.model.backendFramework === 'quarkus') {
      this.model.clientFramework = 'vue';
      this.model.blueprints = [{ name: 'generator-jhipster-quarkus' }];
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.websocket = false;
      this.model.enableSwaggerCodegen = false;
    } else if (this.model.backendFramework === 'micronaut') {
      this.model.blueprints = [{ name: 'generator-jhipster-micronaut' }];
    } else if (this.model.backendFramework === 'rust') {
      this.model.blueprints = [{ name: 'generator-jhipster-rust' }];
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.websocket = false;
      this.model.enableSwaggerCodegen = false;
      this.coerceRustDevDatabase();
    } else if (this.model.backendFramework === 'dotnet') {
      this.model.blueprints = [{ name: 'generator-jhipster-dotnetcore' }];
    } else if (this.model.backendFramework === 'azure-aca') {
      this.model.blueprints = [{ name: 'generator-jhipster-azure-container-apps' }];
    } else if (this.model.backendFramework === 'node') {
      this.model.blueprints = [{ name: 'generator-jhipster-nodejs' }];
      this.coerceNodeDevDatabaseForTypeorm();
    } else if (this.model.backendFramework === 'python') {
      this.model.blueprints = [];
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.websocket = false;
      this.model.enableSwaggerCodegen = false;
    } else {
      this.model.blueprints = [];
    }

    if (this.model.websocket && this.model.backendFramework !== 'quarkus') {
      this.model.websocket = 'spring-websocket';
    }

    if (this.model.cacheProvider === 'no') {
      this.model.enableHibernateCache = false;
    }
    if (this.model.searchEngine) {
      this.model.searchEngine = 'elasticsearch';
    }
    if (this.model.enableSwaggerCodegen && this.model.backendFramework !== 'quarkus') {
      this.model.enableSwaggerCodegen = 'true';
    }
    if (this.model.messageBroker) {
      this.model.messageBroker = 'kafka';
    }
    if (this.model.enableTranslation && !this.model.languages.includes(this.model.nativeLanguage)) {
      this.model.languages.push(this.model.nativeLanguage);
    }
    this.model.jhiPrefix = 'jhi';
    this.coerceRustDevDatabase();
  }

  onSubmit(): void {
    this.checkModelBeforeSubmit();

    if (this.selectedGitProvider && this.selectedGitCompany && this.repositoryName) {
      this.generatorService
        .generateOnGit(this.model, this.selectedGitProvider, this.selectedGitCompany, this.repositoryName, {
          ...(this.openshiftGeneratorApplication ? {} : { kubernetesExtrasYaml: this.kubernetesExtrasYaml }),
          openshiftGeneratorApplication: this.openshiftGeneratorApplication
        })
        .subscribe(
          (res: any) => {
            this.openOutputModal(res);
            this.submitted = false;
          },
          (error: any) => {
            console.error('Error generating the application.');
            console.error(error);
          }
        );
    }
  }

  onSubmitDownload(): void {
    this.checkModelBeforeSubmit();
    this.generatorService
      .download(this.model, this.openshiftGeneratorApplication ? {} : { kubernetesExtrasYaml: this.kubernetesExtrasYaml })
      .subscribe(
        (data: any) => this.downloadFile(data.body),
        // eslint-disable-next-line no-console
        (error: any) => console.log(error),
        () => {
          // eslint-disable-next-line no-console
          console.log('Application downloaded');
          this.submitted = false;
        }
      );
  }

  openOutputModal(applicationId: string): void {
    const modalRef = this.modalService.open(GeneratorOutputDialogComponent, { size: 'lg', backdrop: 'static' }).componentInstance;

    modalRef.applicationId = applicationId;
    modalRef.selectedGitProvider = this.selectedGitProvider;
    modalRef.selectedGitCompany = this.selectedGitCompany;
    modalRef.repositoryName = this.repositoryName;
    modalRef.gitlabHost = this.gitConfig!.gitlabHost;
    modalRef.githubHost = this.gitConfig!.githubHost;
    modalRef.giteaHost = this.gitConfig!.giteaHost;
    modalRef.gitlabConfigured = this.gitConfig!.gitlabAvailable;
    modalRef.githubConfigured = this.gitConfig!.githubAvailable;
    modalRef.giteaConfigured = this.gitConfig!.giteaAvailable;
    modalRef.onGenerationComplete = this.generationFinishedCallback;
  }

  downloadFile(blob: Blob): void {
    const a = document.createElement('a'),
      fileURL = URL.createObjectURL(blob);

    a.href = fileURL;
    a.download = this.model.baseName + '.zip';
    window.document.body.appendChild(a);
    a.click();
    window.document.body.removeChild(a);
    URL.revokeObjectURL(fileURL);
  }

  newGenerator(): void {
    this.model = new JHipsterConfigurationModel(this.defaultModel);
    this.repositoryName = 'jhipster-sample-application';
    this.changeBackendFramework();
  }

  changeApplicationType(): void {
    // server port
    if (this.model.applicationType === 'microservice') {
      this.model.serverPort = 8081;
    } else {
      this.model.serverPort = 8080;
    }
    // authentication
    if (this.model.applicationType !== 'microservice') {
      this.model.authenticationType = 'jwt';
    }
    // service discovery
    if (this.model.applicationType === 'gateway' || this.model.applicationType === 'microservice') {
      this.model.serviceDiscoveryType = 'consul';
    }
    // database
    if (this.model.databaseType === 'no') {
      this.model.databaseType = 'sql';
      this.changeDatabaseType();
    }
    // cache
    if (this.model.applicationType === 'microservice') {
      this.model.cacheProvider = 'hazelcast';
      this.model.enableHibernateCache = true;
    }
  }

  changePackageName(): void {
    this.model.packageFolder = this.model.packageName.replace(/\./g, '/');
  }

  changeBackendFramework(): void {
    const bp = (name: string): BlueprintModel[] => [{ name }];

    this.model.blueprints = [];
    this.model.clientFramework = 'angularX';
    this.model.cacheProvider = 'ehcache';
    this.model.enableHibernateCache = true;
    this.model.websocket = false;
    this.model.enableSwaggerCodegen = false;

    if (this.model.backendFramework === 'quarkus') {
      this.model.clientFramework = 'vue';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.blueprints = bp('generator-jhipster-quarkus');
    } else if (this.model.backendFramework === 'micronaut') {
      this.model.blueprints = bp('generator-jhipster-micronaut');
    } else if (this.model.backendFramework === 'rust') {
      this.model.blueprints = bp('generator-jhipster-rust');
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.websocket = false;
      this.model.enableSwaggerCodegen = false;
      if (this.model.databaseType === 'sql') {
        this.model.prodDatabaseType = 'sqlite';
        this.model.devDatabaseType = 'sqlite';
      }
    } else if (this.model.backendFramework === 'dotnet') {
      this.model.clientFramework = 'angularX';
      this.model.blueprints = bp('generator-jhipster-dotnetcore');
    } else if (this.model.backendFramework === 'azure-aca') {
      this.model.clientFramework = 'angularX';
      this.model.blueprints = bp('generator-jhipster-azure-container-apps');
    } else if (this.model.backendFramework === 'node') {
      this.model.clientFramework = 'angularX';
      this.model.blueprints = bp('generator-jhipster-nodejs');
      this.coerceNodeDevDatabaseForTypeorm();
    } else if (this.model.backendFramework === 'python') {
      this.model.clientFramework = 'angularX';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.blueprints = [];
    }
  }

  changeServiceDiscoveryType(): void {
    if (this.model.serviceDiscoveryType === 'eureka') {
      this.model.authenticationType = 'jwt';
    }
    if (this.model.serviceDiscoveryType === 'false') {
      this.model.serviceDiscoveryType = false;
    }
  }

  changeAuthenticationType(): void {
    this.model.databaseType = 'sql';
    this.model.clientFramework = 'angularX';
    this.changeDatabaseType();
  }

  changeDatabaseType(): void {
    if (this.model.databaseType === 'sql') {
      this.model.prodDatabaseType = AllProdDatabaseTypes.find(type => !this.isProdDatabaseOptionHidden('sql', type)) ?? 'mysql';
      this.model.devDatabaseType = AllDevDatabaseTypes.find(type => !this.isDevDatabaseOptionHidden('sql', type)) ?? 'h2Disk';
      this.model.cacheProvider = 'ehcache';
      this.model.enableHibernateCache = true;
      this.coerceNodeDevDatabaseForTypeorm();
      this.coerceRustDevDatabase();
    } else if (this.model.databaseType === 'mongodb') {
      this.model.prodDatabaseType = 'mongodb';
      this.model.devDatabaseType = 'mongodb';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
    } else if (this.model.databaseType === 'cassandra') {
      this.model.prodDatabaseType = 'cassandra';
      this.model.devDatabaseType = 'cassandra';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.searchEngine = false;
    } else if (this.model.databaseType === 'couchbase') {
      this.model.prodDatabaseType = 'couchbase';
      this.model.devDatabaseType = 'couchbase';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.searchEngine = false;
    } else if (this.model.databaseType === 'no') {
      this.model.devDatabaseType = 'no';
      this.model.prodDatabaseType = 'no';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
      this.model.searchEngine = false;
    }
  }

  changeProdDatabaseType(): void {
    if (this.model.devDatabaseType === this.model.prodDatabaseType) {
      return;
    }

    if (this.model.databaseType === 'sql') {
      // Find first allowed dev database type
      this.model.devDatabaseType = AllDevDatabaseTypes.find(type => !this.isDevDatabaseOptionHidden('sql', type)) ?? 'h2Disk';
      this.coerceNodeDevDatabaseForTypeorm();
      this.coerceRustDevDatabase();
    } else if (this.model.prodDatabaseType === 'mongodb') {
      this.model.devDatabaseType = 'mongodb';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
    } else if (this.model.prodDatabaseType === 'cassandra') {
      this.model.devDatabaseType = 'cassandra';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
    } else if (this.model.prodDatabaseType === 'no') {
      this.model.devDatabaseType = 'no';
      this.model.cacheProvider = 'no';
      this.model.enableHibernateCache = false;
    }
  }

  isAuthenticated(): boolean {
    return this.accountService.isAuthenticated();
  }

  isProdDatabaseOptionHidden(validDatabaseType: string, databaseName: ProdDatabaseType): boolean {
    if (databaseName === 'sqlite' && this.model.backendFramework !== 'rust') {
      return true;
    }
    if (this.model.backendFramework === 'rust' && validDatabaseType === 'sql') {
      const unsupportedRustSql: ProdDatabaseType[] = ['cassandra', 'couchbase', 'neo4j', 'no'];
      if (unsupportedRustSql.includes(databaseName)) {
        return true;
      }
    }
    return this.model.databaseType !== validDatabaseType || Boolean(this.config?.hideProdDatabaseTypeOptions?.includes(databaseName));
  }

  isDatabaseTypeOptionHidden(databaseType: DatabaseType): boolean {
    return Boolean(this.config?.hideDatabaseTypeOptions?.includes(databaseType));
  }

  isDevDatabaseOptionHidden(validDatabaseType: string, databaseName: DevDatabaseType): boolean {
    if (databaseName === 'sqlite' && this.model.backendFramework !== 'rust') {
      return true;
    }
    if (this.model.backendFramework === 'rust' && (databaseName === 'h2Disk' || databaseName === 'h2Memory')) {
      return true;
    }
    if (this.model.backendFramework === 'node' && (databaseName === 'h2Disk' || databaseName === 'h2Memory')) {
      return true;
    }
    return (
      this.model.databaseType !== validDatabaseType ||
      Boolean(this.config?.hideDevDatabaseTypeOptions?.includes(databaseName)) ||
      (databaseName !== 'h2Disk' && databaseName !== 'h2Memory' && this.model.prodDatabaseType !== databaseName)
    );
  }

  /** NestJS + TypeORM do not support Java H2; align dev DB with a real SQL driver. */
  private coerceNodeDevDatabaseForTypeorm(): void {
    if (this.model.backendFramework !== 'node' || this.model.databaseType !== 'sql') {
      return;
    }
    if (this.model.devDatabaseType === 'h2Disk' || this.model.devDatabaseType === 'h2Memory') {
      this.model.devDatabaseType = this.resolveNodeDevDatabaseType();
    }
  }

  private resolveNodeDevDatabaseType(): DevDatabaseType {
    const p = this.model.prodDatabaseType;
    if (p === 'mysql' || p === 'mariadb' || p === 'postgresql' || p === 'oracle' || p === 'mssql') {
      return p;
    }
    return 'mysql';
  }

  /**
   * generator-jhipster-rust does not handle Java H2 dev DB; map to SQLite or a concrete SQL prod engine.
   */
  private coerceRustDevDatabase(): void {
    if (this.model.backendFramework !== 'rust') {
      return;
    }
    if (this.model.databaseType === 'mongodb') {
      this.model.devDatabaseType = 'mongodb';
      this.model.prodDatabaseType = 'mongodb';
      return;
    }
    if (this.model.databaseType !== 'sql') {
      return;
    }
    if (this.model.devDatabaseType === 'h2Disk' || this.model.devDatabaseType === 'h2Memory') {
      const p = this.model.prodDatabaseType;
      if (p === 'mysql' || p === 'mariadb' || p === 'postgresql' || p === 'oracle' || p === 'mssql' || p === 'sqlite') {
        this.model.devDatabaseType = p;
      } else {
        this.model.devDatabaseType = 'sqlite';
        this.model.prodDatabaseType = 'sqlite';
      }
    }
  }
}
