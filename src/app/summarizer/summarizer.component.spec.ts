import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SummarizerComponent } from './summarizer.component';
import { HttpClient, HttpErrorResponse } from '@angular/common/http'; // Import HttpClient if not already

describe('SummarizerComponent', () => {
  let component: SummarizerComponent;
  let fixture: ComponentFixture<SummarizerComponent>;
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SummarizerComponent ],
      imports: [ HttpClientTestingModule ] // Import HttpClientTestingModule
    })
    .compileComponents();

    fixture = TestBed.createComponent(SummarizerComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient); // Get instance of HttpClient
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify(); // Verify that no unmatched requests are outstanding
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onFileSelected', () => {
    it('should set selectedFile for .txt files', () => {
      const mockFile = new File(['dummy content'], 'test.txt', { type: 'text/plain' });
      const mockEvent = { currentTarget: { files: [mockFile], value: '' } as unknown } as Event;
      component.onFileSelected(mockEvent);
      expect(component.selectedFile).toEqual(mockFile);
      expect(component.errorMessage).toBe('');
    });

    it('should set errorMessage for non-.txt files and reset selectedFile', () => {
      const mockFile = new File(['dummy content'], 'image.png', { type: 'image/png' });
      const mockEvent = { currentTarget: { files: [mockFile], value: '' } as unknown } as Event;
      component.onFileSelected(mockEvent);
      expect(component.selectedFile).toBeNull();
      expect(component.errorMessage).toBe('Invalid file type. Please select a .txt file.');
      // expect((mockEvent.currentTarget as HTMLInputElement).value).toBe(''); // Check if input value is reset
    });

    it('should clear selectedFile if no file is selected', () => {
      const mockEvent = { currentTarget: { files: [], value: '' } as unknown } as Event;
      component.onFileSelected(mockEvent);
      expect(component.selectedFile).toBeNull();
    });
  });

  describe('uploadAndSummarize', () => {
    const backendUrl = 'http://localhost:8080/api/summarize'; // Match component's URL

    it('should set errorMessage if no file is selected', () => {
      component.selectedFile = null;
      component.uploadAndSummarize();
      expect(component.errorMessage).toBe('Please select a file first.');
      expect(component.isLoading).toBe(false);
    });

    it('should call backend API and set summary on success', fakeAsync(() => {
      const mockFile = new File(['file content'], 'test.txt', { type: 'text/plain' });
      component.selectedFile = mockFile;
      const mockSummary = 'This is a summary.';

      component.uploadAndSummarize();
      expect(component.isLoading).toBe(true);

      const req = httpMock.expectOne(backendUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.get('file')).toEqual(mockFile);
      req.flush(mockSummary); // Simulate successful response

      tick(); // Process observables

      expect(component.isLoading).toBe(false);
      expect(component.summary).toBe(mockSummary);
      expect(component.errorMessage).toBe('');
      // expect(component.selectedFile).toBeNull(); // Input should be cleared
    }));

    it('should set errorMessage on API error (string error)', fakeAsync(() => {
      const mockFile = new File(['file content'], 'error.txt', { type: 'text/plain' });
      component.selectedFile = mockFile;
      const errorResponse = new HttpErrorResponse({
        error: 'Server-side error message',
        status: 500,
        statusText: 'Internal Server Error'
      });

      component.uploadAndSummarize();
      expect(component.isLoading).toBe(true);

      const req = httpMock.expectOne(backendUrl);
      req.error(new ErrorEvent('network error'), errorResponse); // Simulate error response

      tick();

      expect(component.isLoading).toBe(false);
      expect(component.summary).toBe('');
      expect(component.errorMessage).toContain('Error from server: Server-side error message');
      // expect(component.selectedFile).toBeNull();
    }));

    it('should set specific errorMessage if API key not configured', fakeAsync(() => {
      const mockFile = new File(['file content'], 'apikey.txt', { type: 'text/plain' });
      component.selectedFile = mockFile;
      const errorResponse = new HttpErrorResponse({
        error: 'OpenAI API key not configured on the server.',
        status: 400, // Or whatever status backend returns
        statusText: 'Bad Request'
      });

      component.uploadAndSummarize();
      const req = httpMock.expectOne(backendUrl);
      req.error(new ErrorEvent('network error'), errorResponse);
      tick();

      expect(component.errorMessage).toBe('Error: The OpenAI API key is not configured on the server. Please contact the administrator.');
    }));

    it('should set errorMessage for connection refused (status 0)', fakeAsync(() => {
        const mockFile = new File(['file content'], 'connection.txt', { type: 'text/plain' });
        component.selectedFile = mockFile;
        const errorResponse = new HttpErrorResponse({
          error: new ProgressEvent('error'), // type ProgressEvent to simulate network error
          status: 0, // Status 0 for connection refused
          statusText: 'Unknown Error'
        });

        component.uploadAndSummarize();
        const req = httpMock.expectOne(backendUrl);
        req.error(new ErrorEvent('network error'), errorResponse);
        tick();

        expect(component.errorMessage).toBe('Error: Could not connect to the backend. Please ensure it is running and accessible.');
      }));
  });

  // Helper to simulate file input
  function createFileInputEvent(file: File): Event {
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    const event = new Event('change', { bubbles: true });
    Object.defineProperty(event, 'target', {
        writable: false,
        value: { files: dataTransfer.files, value: file.name }
    });
    Object.defineProperty(event, 'currentTarget', { // also mock currentTarget
        writable: false,
        value: { files: dataTransfer.files, value: file.name }
    });
    return event;
  }
});
