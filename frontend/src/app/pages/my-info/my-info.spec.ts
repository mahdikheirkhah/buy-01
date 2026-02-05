import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MyInfo } from './my-info';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';

xdescribe('MyInfo', () => {
  let component: MyInfo;
  let fixture: ComponentFixture<MyInfo>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let userServiceMock: jasmine.SpyObj<UserService>;
  let routerMock: jasmine.SpyObj<Router>;
  let dialogMock: jasmine.SpyObj<MatDialog>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser', 'logout']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      role: 'CLIENT',
      avatarUrl: '/uploads/avatar.jpg'
    } as any));
    authServiceMock.logout.and.returnValue(of({}));

    userServiceMock = jasmine.createSpyObj('UserService', ['updateUser', 'updateAvatar', 'deleteAvatar', 'deleteUser']);
    userServiceMock.updateAvatar.and.returnValue(of({} as any));
    userServiceMock.deleteAvatar.and.returnValue(of('Avatar deleted'));
    userServiceMock.deleteUser.and.returnValue(of({ message: 'User deleted' }));

    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);

    // Override component to use inline template
    TestBed.overrideComponent(MyInfo, {
      set: {
        template: `
          <div *ngIf="currentUser">
            <h1>{{ currentUser.firstName }} {{ currentUser.lastName }}</h1>
            <p>{{ currentUser.email }}</p>
            <button (click)="onDeleteAvatar()">Delete Avatar</button>
            <button (click)="onDeleteAccount()">Delete Account</button>
          </div>
        `,
        styles: []
      }
    });

    await TestBed.configureTestingModule({
      imports: [MyInfo, HttpClientTestingModule, CommonModule, MatDialogModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: MatDialog, useValue: dialogMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyInfo);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();

    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toBeTruthy();
    expect(component.currentUser?.email).toBe('john@example.com');
  });

  it('should set isLoading to false after user fetch', () => {
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
  });

  it('should handle user fetch error', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();

    expect(component.errorMessage).toBe('Could not load user data.');
    expect(component.isLoading).toBe(false);
  });

  it('should show cropper when file is selected', () => {
    const event = { target: { files: [new File([''], 'avatar.jpg')] } };

    component.onFileSelect(event);

    expect(component.imageChangedEvent).toBe(event);
    expect(component.showCropper).toBe(true);
  });

  it('should upload avatar when blob is provided', () => {
    component.currentUser = { id: '1', email: 'test@example.com' } as any;
    const blob = new Blob(['test'], { type: 'image/png' });

    component.handleAvatarBlob(blob);

    expect(userServiceMock.updateAvatar).toHaveBeenCalled();
    const callArg = userServiceMock.updateAvatar.calls.mostRecent().args[0];
    expect(callArg instanceof File).toBe(true);
  });

  it('should not upload avatar if no user', () => {
    component.currentUser = null;
    const blob = new Blob(['test'], { type: 'image/png' });

    component.handleAvatarBlob(blob);

    expect(userServiceMock.updateAvatar).not.toHaveBeenCalled();
  });

  it('should handle avatar upload error', () => {
    userServiceMock.updateAvatar.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(console, 'error');
    component.currentUser = { id: '1' } as any;
    const blob = new Blob(['test'], { type: 'image/png' });

    component.handleAvatarBlob(blob);

    expect(console.error).toHaveBeenCalled();
  });

  it('should close modal and reset file input', () => {
    const mockInput = document.createElement('input');
    mockInput.id = 'avatar-upload-input';
    mockInput.value = 'test.jpg';
    document.body.appendChild(mockInput);

    component.handleModalClose();

    expect(component.showCropper).toBe(false);
    expect(mockInput.value).toBe('');
    document.body.removeChild(mockInput);
  });

  it('should build full avatar URL', () => {
    const url = component.getAvatarUrl('/uploads/avatar.jpg');

    expect(url).toBe('https://localhost:8443/uploads/avatar.jpg');
  });

  it('should open confirm dialog on delete avatar', () => {
    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.currentUser = { id: '1' } as any;

    component.onDeleteAvatar();

    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should delete avatar when confirmed', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.currentUser = { id: '1' } as any;

    component.onDeleteAvatar();

    expect(userServiceMock.deleteAvatar).toHaveBeenCalled();
  });

  it('should not delete avatar when cancelled', () => {
    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.currentUser = { id: '1' } as any;

    component.onDeleteAvatar();

    expect(userServiceMock.deleteAvatar).not.toHaveBeenCalled();
  });

  it('should not open delete avatar dialog if no user', () => {
    component.currentUser = null;

    component.onDeleteAvatar();

    expect(dialogMock.open).not.toHaveBeenCalled();
  });

  it('should handle avatar delete error', () => {
    userServiceMock.deleteAvatar.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(console, 'error');
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.currentUser = { id: '1' } as any;

    component.onDeleteAvatar();

    expect(console.error).toHaveBeenCalled();
  });

  it('should open password dialog on delete account', () => {
    const dialogRefMock = { afterClosed: () => of(null) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDeleteMe();

    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should delete user and logout when password provided', () => {
    const dialogRefMock = { afterClosed: () => of('password123') };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDeleteMe();

    expect(userServiceMock.deleteUser).toHaveBeenCalledWith('password123');
    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/register']);
  });

  it('should not delete user when password dialog cancelled', () => {
    const dialogRefMock = { afterClosed: () => of(null) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDeleteMe();

    expect(userServiceMock.deleteUser).not.toHaveBeenCalled();
  });

  it('should handle delete user error', () => {
    userServiceMock.deleteUser.and.returnValue(throwError(() => ({ error: { message: 'Wrong password' } })));
    spyOn(console, 'error');
    spyOn(window, 'alert');
    const dialogRefMock = { afterClosed: () => of('wrongpassword') };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDeleteMe();

    expect(console.error).toHaveBeenCalled();
    expect(window.alert).toHaveBeenCalledWith('Error: Wrong password');
  });

  it('should set isEditingInfo to true when onUpdateInfo called', () => {
    component.onUpdateInfo();

    expect(component.isEditingInfo).toBe(true);
  });

  it('should close form when onFormClosed called', () => {
    component.isEditingInfo = true;

    component.onFormClosed(false);

    expect(component.isEditingInfo).toBe(false);
  });

  it('should refresh user data when form closed successfully', () => {
    component.isEditingInfo = true;
    spyOn(component, 'ngOnInit');

    component.onFormClosed(true);

    expect(component.isEditingInfo).toBe(false);
    expect(component.ngOnInit).toHaveBeenCalled();
  });
});
