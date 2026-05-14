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

interface GitRuntimeConfigView {
  githubHost: string;
  githubClientId: string;
  githubClientSecretSet: boolean;
  gitlabHost: string;
  gitlabClientId: string;
  gitlabRedirectUri: string;
  gitlabClientSecretSet: boolean;
  giteaHost: string;
  giteaClientId: string;
  giteaRedirectUri: string;
  giteaClientSecretSet: boolean;
}

@Component({
  standalone: false,
  selector: 'jhi-git-runtime-config',
  templateUrl: './git-runtime-config.component.html'
})
export class GitRuntimeConfigComponent implements OnInit {
  loaded = false;
  loadError = '';
  saveMessage = '';
  saveError = '';

  view: GitRuntimeConfigView = {
    githubHost: '',
    githubClientId: '',
    githubClientSecretSet: false,
    gitlabHost: '',
    gitlabClientId: '',
    gitlabRedirectUri: '',
    gitlabClientSecretSet: false,
    giteaHost: '',
    giteaClientId: '',
    giteaRedirectUri: '',
    giteaClientSecretSet: false
  };

  githubClientSecret = '';
  gitlabClientSecret = '';
  giteaClientSecret = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loadError = '';
    this.http.get<GitRuntimeConfigView>('api/git/admin/runtime-config').subscribe(
      v => {
        this.view = v;
        this.githubClientSecret = '';
        this.gitlabClientSecret = '';
        this.giteaClientSecret = '';
        this.loaded = true;
      },
      () => {
        this.loadError = 'Could not load runtime Git configuration.';
        this.loaded = true;
      }
    );
  }

  save(): void {
    this.saveMessage = '';
    this.saveError = '';
    const body: Record<string, string> = {
      githubHost: this.view.githubHost,
      githubClientId: this.view.githubClientId,
      gitlabHost: this.view.gitlabHost,
      gitlabClientId: this.view.gitlabClientId,
      gitlabRedirectUri: this.view.gitlabRedirectUri,
      giteaHost: this.view.giteaHost,
      giteaClientId: this.view.giteaClientId,
      giteaRedirectUri: this.view.giteaRedirectUri
    };
    if (this.githubClientSecret.trim().length > 0) {
      body.githubClientSecret = this.githubClientSecret;
    }
    if (this.gitlabClientSecret.trim().length > 0) {
      body.gitlabClientSecret = this.gitlabClientSecret;
    }
    if (this.giteaClientSecret.trim().length > 0) {
      body.giteaClientSecret = this.giteaClientSecret;
    }
    this.http.put('api/git/admin/runtime-config', body).subscribe(
      () => {
        this.saveMessage = 'Saved.';
        this.reload();
      },
      e => {
        this.saveError = e.error?.message || e.message || 'Save failed';
      }
    );
  }
}
