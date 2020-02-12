/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import {CheBranding} from './branding.service';
import {CheService} from '../api/che-service.factory';

/**
 * This class handles configuration data of Dashboard.
 * @author Oleksii Kurinnyi
 */
export class CheDashboardConfigurationService {

  static $inject = [
    '$q',
    'cheBranding',
    'cheService',
    '$rootScope'
  ];

  $q: ng.IQService;
  cheBranding: CheBranding;

  constructor(
    $q: ng.IQService,
    cheBranding: CheBranding,
    cheService: CheService,
    $rootScope: ng.IRootScopeService
  ) {
    this.$q = $q;
    this.cheBranding = cheBranding;

    cheBranding.ready.then(() => {
      cheService.fetchServicesInfo().then(() => {
        const info = cheService.getServicesInfo();
        ($rootScope as any).productVersion = (info && info.buildInfo) ? info.buildInfo : '';
      });
    });
  }

  allowedMenuItem(menuItem: che.ConfigurableMenuItem | string): boolean {
    const disabledItems = this.cheBranding.getConfiguration().menu.disabled;
    return (disabledItems as string[]).indexOf(menuItem) === -1;
  }

  allowRoutes(menuItem: che.ConfigurableMenuItem): ng.IPromise<void> {
    return this.$q.resolve().then(() => {
      this.cheBranding.ready.then(() => {
        if (this.allowedMenuItem(menuItem) === false) {
          return this.$q.reject();
        }
      });
    });
  }

}
