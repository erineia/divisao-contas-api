import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Fechamentos } from './fechamentos';

describe('Fechamentos', () => {
  let component: Fechamentos;
  let fixture: ComponentFixture<Fechamentos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Fechamentos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Fechamentos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
