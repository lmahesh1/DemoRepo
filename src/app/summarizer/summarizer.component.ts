import { Component } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-summarizer',
  templateUrl: './summarizer.component.html',
  styleUrls: ['./summarizer.component.css']
})
export class SummarizerComponent {
  selectedFile: File | null = null;
  summary: string = '';
  errorMessage: string = '';
  isLoading: boolean = false;

  // Backend API endpoint for summarization.
  // Ensure this matches the address where your Java backend is running.
  private backendUrl = 'http://localhost:8080/api/summarize';

  constructor(private http: HttpClient) {}

  /**
   * Handles the 'change' event from the file input element.
   * It validates if a file is selected and if it's a .txt file.
   * Updates component state based on the selected file.
   * @param event The DOM event triggered by the file input.
   */
  onFileSelected(event: Event): void {
    this.errorMessage = ''; // Clear previous errors
    this.summary = '';      // Clear previous summary
    const element = event.currentTarget as HTMLInputElement;
    let fileList: FileList | null = element.files;

    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      if (file.type === "text/plain") { // Basic MIME type check for .txt
        this.selectedFile = file;
      } else {
        this.selectedFile = null;
        this.errorMessage = "Invalid file type. Please select a .txt file.";
        element.value = ''; // Reset the file input to allow re-selection of the same file if needed after error
      }
    } else {
      this.selectedFile = null; // No file selected or selection was cleared
    }
  }

  /**
   * Initiates the file upload and summarization process.
   * It sends the selected .txt file to the backend API.
   * Handles the response by displaying the summary or an error message.
   * Manages the loading state during the API call.
   */
  uploadAndSummarize(): void {
    if (!this.selectedFile) {
      this.errorMessage = "Please select a file first.";
      return;
    }

    this.isLoading = true;
    this.summary = '';
    this.errorMessage = '';

    const formData = new FormData();
    formData.append('file', this.selectedFile, this.selectedFile.name);

    // Make the POST request to the backend
    this.http.post(this.backendUrl, formData, { responseType: 'text' }) // Expecting plain text summary
      .subscribe(
        (response: string) => {
          this.summary = response;
          this.isLoading = false;
          this.clearFileInput(); // Clear input after successful processing
        },
        (error: HttpErrorResponse) => {
          console.error('Backend error:', error); // Log the full error for debugging
          this.isLoading = false;
          this.clearFileInput(); // Clear input even on error

          // Provide user-friendly error messages
          if (error.status === 0) {
            // This typically means the backend is not reachable (network error, server down)
            this.errorMessage = 'Error: Could not connect to the backend. Please ensure it is running and accessible.';
          } else if (error.error && typeof error.error === 'string') {
            // Error message is a string from the backend (e.g., custom error response)
            if (error.error.includes("OpenAI API key not configured")) {
                this.errorMessage = "Error: The OpenAI API key is not configured on the server. Please contact the administrator.";
            } else if (error.error.includes("File content is empty")) {
                this.errorMessage = "Error: The uploaded file content is empty or contains only whitespace.";
            } else if (error.error.includes("Invalid file type")) {
                this.errorMessage = "Error: Invalid file type. Only .txt files are allowed.";
            }
            else {
                this.errorMessage = `Error from server: ${error.error}`;
            }
          } else {
            // Generic error for other cases (e.g., unexpected server error structure)
            this.errorMessage = `An unexpected error occurred: ${error.statusText} (Status: ${error.status})`;
          }
        }
      );
  }

  /**
   * Resets the selected file state and clears the visual file input element.
   * This is a private helper method for use after an upload attempt.
   */
  private clearFileInput(): void {
    this.selectedFile = null;
    // Attempt to reset the file input DOM element
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = ''; // This clears the displayed file name in the input
    }
  }
}
