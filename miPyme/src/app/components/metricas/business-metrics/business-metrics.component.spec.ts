import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BusinessMetricsComponent } from './business-metrics.component';

describe('BusinessMetricsComponent', () => {
  let component: BusinessMetricsComponent;
  let fixture: ComponentFixture<BusinessMetricsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BusinessMetricsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BusinessMetricsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
