import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { SidenavComponent } from './sidenav';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';

describe('SidenavComponent', () => {
  let component: SidenavComponent;
  let fixture: ComponentFixture<SidenavComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;
  let currentUserSubject: BehaviorSubject<User | null>;

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<User | null>(null);
    authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.logout.and.returnValue(of(void 0));
    Object.defineProperty(authServiceMock, 'currentUser$', {
      get: () => currentUserSubject.asObservable()
    });
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [SidenavComponent, HttpClientTestingModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SidenavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call isSeller and return role from authService', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'SELLER',
      configurable: true
    });

    const result = component.isSeller();

    expect(result).toBe(true);
  });

  it('should return false from isSeller when not SELLER', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'CLIENT',
      configurable: true
    });

    const result = component.isSeller();

    expect(result).toBe(false);
  });

  it('should call logout, emit closeSidenav, and navigate', () => {
    spyOn(component.closeSidenav, 'emit');

    component.logout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(component.closeSidenav.emit).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });
});
