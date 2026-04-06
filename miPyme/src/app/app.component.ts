import { Component, OnInit } from '@angular/core';
import { ThemeService } from './services/theme.service';
import { environment } from '../environments/environment';

declare var FB: any;

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'miPyme';

  constructor(private themeService: ThemeService) {

  }

  ngOnInit(): void {
    (window as any).fbAsyncInit = () => {
      FB.init({
        appId: environment.facebookAppId,
        autoLogAppEvents: true,
        xfbml: true,
        version: environment.facebookSdkVersion
      });
      console.log('[FB SDK] Initialized successfully');
    };
  }
}
