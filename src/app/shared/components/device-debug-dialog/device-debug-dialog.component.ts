import {Component, Inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';

@Component({
  selector: 'app-device-debug-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './device-debug-dialog.component.html',
  styleUrl: './device-debug-dialog.component.scss'
})
export class DeviceDebugDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<DeviceDebugDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
