import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {DeviceManagementComponent} from '../../shared/components/device-management/device-management.component';

@Component({
  selector: 'app-devices',
  standalone: true,
  imports: [
    CommonModule,
    DeviceManagementComponent
  ],
  templateUrl: './devices.component.html',
  styleUrl: './devices.component.scss'
})
export class DevicesComponent {
}
