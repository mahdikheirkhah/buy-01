import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyInfo } from './my-info';

describe('MyInfo', () => {
  let component: MyInfo;
  let fixture: ComponentFixture<MyInfo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyInfo]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyInfo);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
