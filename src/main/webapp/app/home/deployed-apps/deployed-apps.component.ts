import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'jhi-deployed-apps',
  templateUrl: './deployed-apps.component.html'
})
export class DeployedAppsComponent implements OnInit {
  namespace = '';
  namespaces: string[] = [];
  applications: any[] = [];
  loading = false;
  error = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<string[]>('api/openshift/namespaces').subscribe(
      (data: string[]) => {
        this.namespaces = data;
        if (data.length > 0) {
          this.namespace = data[0];
          this.loadApplications();
        }
      },
      () => {
        this.namespaces = [];
      }
    );
  }

  loadApplications(): void {
    if (!this.namespace) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.http.get<any[]>('api/openshift/applications?namespace=' + this.namespace).subscribe(
      (data: any[]) => {
        this.applications = data;
        this.loading = false;
      },
      (err: any) => {
        this.error = 'Failed to load applications: ' + (err.error?.error || err.message);
        this.loading = false;
      }
    );
  }

  deleteApp(name: string): void {
    if (!confirm('Delete application ' + name + '?')) {
      return;
    }
    this.http.delete('api/openshift/applications/' + name + '?namespace=' + this.namespace).subscribe(
      () => {
        this.loadApplications();
      },
      (err: any) => {
        this.error = 'Failed to delete: ' + (err.error?.error || err.message);
      }
    );
  }
}
