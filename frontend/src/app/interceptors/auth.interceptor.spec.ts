import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn, HttpRequest, HttpHandler, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
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

  it('should handle different content types', (done) => {
    const jsonRequest = new HttpRequest('POST', '/api/test', {});
    TestBed.runInInjectionContext(() => {
      authInterceptor(jsonRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle error responses', (done) => {
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        throwError(() => ({ status: 401, message: 'Unauthorized' }))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe({
        error: (error) => {
          expect(error.status).toBe(401);
          done();
        }
      });
    });
  });

  it('should handle 403 forbidden responses', (done) => {
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        throwError(() => ({ status: 403, message: 'Forbidden' }))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe({
        error: (error) => {
          expect(error.status).toBe(403);
          done();
        }
      });
    });
  });

  it('should handle 404 not found responses', (done) => {
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        throwError(() => ({ status: 404, message: 'Not Found' }))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });
    });
  });

  it('should handle 500 server error responses', (done) => {
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        throwError(() => ({ status: 500, message: 'Server Error' }))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe({
        error: (error) => {
          expect(error.status).toBe(500);
          done();
        }
      });
    });
  });

  it('should handle network timeout errors', (done) => {
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        throwError(() => new Error('Network timeout'))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe({
        error: (error) => {
          expect(error.message).toBe('Network timeout');
          done();
        }
      });
    });
  });

  it('should handle successful response with data', (done) => {
    const responseData = { id: 1, name: 'Test' };
    mockNext = {
      handle: jasmine.createSpy('handle').and.returnValue(
        of(new HttpResponse({ body: responseData }))
      )
    } as any;

    TestBed.runInInjectionContext(() => {
      authInterceptor(mockRequest, mockNext.handle).subscribe((event: any) => {
        expect(event.body).toEqual(responseData);
        done();
      });
    });
  });

  it('should handle requests with custom headers', (done) => {
    const requestWithHeaders = new HttpRequest('POST', '/api/test', {});
    TestBed.runInInjectionContext(() => {
      authInterceptor(requestWithHeaders, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalledWith(jasmine.any(HttpRequest));
        done();
      });
    });
  });

  it('should handle PATCH requests', (done) => {
    const patchRequest = new HttpRequest('PATCH', '/api/test', { field: 'value' });
    TestBed.runInInjectionContext(() => {
      authInterceptor(patchRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle requests with query parameters', (done) => {
    const requestWithParams = new HttpRequest('GET', '/api/test?page=1&limit=10');
    TestBed.runInInjectionContext(() => {
      authInterceptor(requestWithParams, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle requests to different API endpoints', (done) => {
    const endpointRequest = new HttpRequest('GET', '/api/products/1/details');
    TestBed.runInInjectionContext(() => {
      authInterceptor(endpointRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle file upload requests', (done) => {
    const formData = new FormData();
    formData.append('file', new Blob(['test']), 'test.txt');
    const uploadRequest = new HttpRequest('POST', '/api/upload', formData);
    TestBed.runInInjectionContext(() => {
      authInterceptor(uploadRequest, mockNext.handle).subscribe(() => {
        expect(mockNext.handle).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should handle concurrent requests', (done) => {
    const request1 = new HttpRequest('GET', '/api/test1');
    const request2 = new HttpRequest('GET', '/api/test2');

    let count = 0;
    TestBed.runInInjectionContext(() => {
      authInterceptor(request1, mockNext.handle).subscribe(() => {
        count++;
        if (count === 2) done();
      });
      authInterceptor(request2, mockNext.handle).subscribe(() => {
        count++;
        if (count === 2) done();
      });
    });
  });

});
