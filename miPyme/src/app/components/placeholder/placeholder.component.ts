import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  template: `
    <div style="padding: 2rem; text-align: center; color: #5B6472;">
      <h2 style="font-size: 2rem; margin-bottom: 1rem; color: #0B1220;">{{ title }}</h2>
      <p>Módulo en construcción.</p>
      <div style="font-size: 3rem; margin-top: 2rem; opacity: 0.2;">🏗️</div>
    </div>
  `
})
export class PlaceholderComponent implements OnInit {
  title: string = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {

    const path = this.route.snapshot.url[0]?.path;
    this.title = path ? path.charAt(0).toUpperCase() + path.slice(1) : 'Dashboard';
  }
}
