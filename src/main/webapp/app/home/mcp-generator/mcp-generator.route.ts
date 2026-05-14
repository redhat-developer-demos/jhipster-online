import { Route } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { McpGeneratorComponent } from './mcp-generator.component';

export const MCP_GENERATOR_ROUTE: Route = {
  path: 'generate-mcp-server',
  component: McpGeneratorComponent,
  canActivate: [UserRouteAccessService],
  data: {
    authorities: ['ROLE_USER'],
    pageTitle: 'MCP Server Generator'
  }
};
