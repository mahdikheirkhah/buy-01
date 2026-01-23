import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => authInterceptor(req, next));

  let mockRequest: HttpRequest<any>;
  let mockNext: HttpHandler;

  beforeEach(() => {
    TestBed.configureTestingModule({});

    mockRequest = new HttpRequest('GET', '/api/test');
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(of({} as HttpEvent<any>))
    } as any;
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  it('should pass through the request', (done) => {
    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle POST requests', (done) => {
    const postRequest = new HttpRequest('POST', '/api/test', { data: 'test' });
    TestBed.runInInjectionContext(() => {
      authInterceptor(postRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle PUT requests', (done) => {
    const putRequest = new HttpRequest('PUT', '/api/test', { data: 'update' });
    TestBed.runInInjectionContext(() => {
      authInterceptor(putRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle DELETE requests', (done) => {
    const deleteRequest = new HttpRequest('DELETE', '/api/test');
    TestBed.runInInjectionContext(() => {
      authInterceptor(deleteRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

});
