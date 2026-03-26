import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    standalone: false
})
export class LoginComponent {
  loading = false;
  error: string | null = null;

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  submit() {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.error = null;

    const raw = this.form.getRawValue();
    const payload = { username: raw.username ?? '', password: raw.password ?? '' };
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';

    this.auth.login(payload).subscribe({
      next: () => this.router.navigateByUrl(returnUrl),
      error: (err) => {
        this.error =
          err?.error?.message || 'Login failed. Check your credentials.';
        this.loading = false;
      },
    });
  }
}
