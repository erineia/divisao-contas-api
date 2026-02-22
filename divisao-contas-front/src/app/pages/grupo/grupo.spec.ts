import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrupoComponent } from './grupo';

describe('GrupoComponent', () => {
  let component: GrupoComponent;
  let fixture: ComponentFixture<GrupoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GrupoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrupoComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
