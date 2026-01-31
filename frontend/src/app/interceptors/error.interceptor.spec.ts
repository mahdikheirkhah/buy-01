import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse, HttpEvent } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { throwError, of, Observable } from 'rxjs';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let snackBarMock: jasmine.SpyObj<MatSnackBar>;
  let routerMock: jasmine.SpyObj<Router>;
  let mockRequest: HttpRequest<any>;
  let mockNext: jasmine.Spy<HttpHandlerFn>;

  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => errorInterceptor(req, next));

  beforeEach(() => {
    snackBarMock = jasmine.createSpyObj('MatSnackBar', ['open']);
    snackBarMock.open.and.returnValue({ dismiss: jasmine.createSpy('dismiss') } as any);
    routerMock = jasmine.createSpyObj('Router', ['navigate'], { events: of() });

    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: snackBarMock },
        { provide: Router, useValue: routerMock }
      ]
    });

    mockRequest = new HttpRequest('GET', '/api/test');
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  it('should handle 401 errors and redirect to login', (done) => {
    const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(throwError(() => error));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe({
        error: () => {
          expect(snackBarMock.open).toHaveBeenCalledWith('Session expired. Please log in.', 'Close', jasmine.any(Object));
          expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
          done();
        }
      });
    });
  });

  it('should handle 403 errors', (done) => {
    const error = new HttpErrorResponse({ status: 403, statusText: 'Forbidden' });
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(throwError(() => error));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe({
        error: () => {
          expect(snackBarMock.open).toHaveBeenCalledWith('You are not authorized.', 'Close', jasmine.any(Object));
          done();
        }
      });
    });
  });

  it('should handle 404 errors', (done) => {
    const error = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(throwError(() => error));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe({
        error: () => {
          expect(snackBarMock.open).toHaveBeenCalledWith('Resource not found.', 'Close', jasmine.any(Object));
          done();
        }
      });
    });
  });

  it('should handle 500 errors', (done) => {
    const error = new HttpErrorResponse({ status: 500, statusText: 'Server Error', error: { message: 'Server error' } });
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(throwError(() => error));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe({
        error: () => {
          expect(snackBarMock.open).toHaveBeenCalledWith('Server error', 'Close', jasmine.any(Object));
          done();
        }
      });
    });
  });

  it('should handle generic errors', (done) => {
    const error = new HttpErrorResponse({ status: 400, statusText: 'Bad Request', error: { message: 'Bad request error' } });
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(throwError(() => error));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe({
        error: () => {
          expect(snackBarMock.open).toHaveBeenCalledWith('Bad request error', 'Close', jasmine.any(Object));
          done();
        }
      });
    });
  });

  it('should pass through successful requests', (done) => {
    mockNext = jasmine.createSpy<HttpHandlerFn>('next').and.returnValue(of({} as HttpEvent<any>));

    TestBed.runInInjectionContext(() => {
      errorInterceptor(mockRequest, mockNext).subscribe(() => {
        expect(mockNext).toHaveBeenCalled();
        expect(snackBarMock.open).not.toHaveBeenCalled();
        done();
      });
    });
  });
});

