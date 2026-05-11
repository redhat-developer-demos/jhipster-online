import { Route } from '@angular/router';

import { DeployedAppsComponent } from './deployed-apps.component';

export const DEPLOYED_APPS_ROUTE: Route = {
  path: 'deployed-applications',
  component: DeployedAppsComponent,
  data: {
    authorities: ['ROLE_USER'],
    pageTitle: 'Deployed Applications'
  }
};
