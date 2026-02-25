import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SnackWarningComponent } from './snack-warning.component';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  constructor(private snackBar: MatSnackBar) {}

  success(message: string, duration = 4000) {
    this.snackBar.open(message, '×', {
      duration,
      panelClass: ['snackbar-success'],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  error(message: string, duration = 4000) {
    this.snackBar.open(message, '×', {
      duration,
      panelClass: ['snackbar-error'],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  warn(message: string, duration = 8000) {
    // add a class to the overlay container so we can style the snackbar
    const overlay = document.querySelector('.cdk-overlay-container') as HTMLElement | null;
    if (overlay) overlay.classList.add('has-warning-snackbar');

    // open a custom component for full control over markup and styling
    const ref = this.snackBar.openFromComponent(SnackWarningComponent, {
      duration,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['snackbar-warning-container', 'has-icon', 'custom-icon'],
      data: { message },
    });

    // Fall back: if openFromComponent couldn't resolve, open a plain snackbar
    if (!ref) {
      const fallback = this.snackBar.open(message, '×', {
        duration,
        panelClass: ['snackbar-warning'],
        horizontalPosition: 'right',
        verticalPosition: 'top',
      });
      fallback.afterDismissed().subscribe(() => {
        if (overlay) overlay.classList.remove('has-warning-snackbar');
      });
      return;
    }

    ref.afterDismissed().subscribe(() => {
      if (overlay) overlay.classList.remove('has-warning-snackbar');
      // remove any inline styles we applied to the snackbar container
      try {
        const container = document.querySelector('.cdk-overlay-container .mat-snack-bar-container') as HTMLElement | null
          || document.querySelector('.cdk-overlay-container .mat-mdc-snack-bar-container') as HTMLElement | null;
        if (container) {
          container.style.background = '';
          container.style.padding = '';
          container.style.boxShadow = '';
        }
      } catch (e) {
        /* ignore */
      }
    });

    // also attempt to force the container transparent immediately (some Material themes render a white box)
    try {
      setTimeout(() => {
        const container = document.querySelector('.cdk-overlay-container .mat-snack-bar-container') as HTMLElement | null
          || document.querySelector('.cdk-overlay-container .mat-mdc-snack-bar-container') as HTMLElement | null;
        if (container) {
          container.style.background = 'transparent';
          container.style.padding = '0';
          container.style.boxShadow = 'none';
        }
      }, 0);
    } catch (e) {
      /* ignore */
    }
    // also force styles on the overlay pane wrapper (rounding/shadow/white background often comes from here)
    try {
      setTimeout(() => {
        const overlayPane = document.querySelector('.cdk-overlay-container .cdk-overlay-pane') as HTMLElement | null
          || document.querySelector('.cdk-global-overlay-wrapper .cdk-overlay-pane') as HTMLElement | null;
        if (overlayPane) {
          overlayPane.style.background = 'transparent';
          overlayPane.style.boxShadow = 'none';
          overlayPane.style.padding = '0';
          overlayPane.style.border = 'none';
        }
      }, 0);
    } catch (e) {
      /* ignore */
    }
  }
}
