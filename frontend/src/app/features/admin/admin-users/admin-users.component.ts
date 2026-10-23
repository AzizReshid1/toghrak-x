import { Component, OnInit } from '@angular/core';
import { UsersService } from '../../../services/users.service';
import { User, CreateUserRequest } from '../../../models/user';
import { UiMessageService } from '../../../services/ui-message.service';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-admin-users',
    templateUrl: './admin-users.component.html',
    imports: [FormsModule]
})
export class AdminUsersComponent implements OnInit {
  users: User[] = [];
  loading = false;
  error: string | null = null;

  form: CreateUserRequest = {
    username: '',
    email: '',
    password: '',
    roleName: 'EDITOR',
  };
  saving = false;

  constructor(private userService: UsersService, private ui: UiMessageService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch() {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users.';
        this.loading = false;
      },
    });
  }

  create() {
    if (this.saving) return;
    this.saving = true;
    this.userService.create(this.form).subscribe({
      next: () => {
        this.form = {
          username: '',
          email: '',
          password: '',
          roleName: 'EDITOR',
        };
        this.fetch();
        this.saving = false;
        this.ui.success('User created.');
      },
      // The reason (e.g. username taken, password too short) is shown as a toast by httpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  delete(id: number) {
    if (!confirm('Delete user?')) return;
    this.userService.delete(id).subscribe({
      next: () => {
        this.fetch();
        this.ui.success('User deleted.');
      },
      // The reason (e.g. the user still owns posts) is shown as a toast by httpErrorInterceptor
      error: () => {},
    });
  }
}
