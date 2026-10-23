import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { UserRoutingModule } from './user-routing.module';
import { ProfileComponent } from './profile/profile.component';

@NgModule({
    imports: [CommonModule, UserRoutingModule, ProfileComponent],
})
export class UserModule {}

