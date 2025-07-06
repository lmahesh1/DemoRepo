import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http'; // Import HttpClientModule
import { FormsModule } from '@angular/forms'; // Import FormsModule for template-driven forms if needed, or ReactiveFormsModule

import { AppComponent } from './app.component';
import { SummarizerComponent } from './summarizer/summarizer.component'; // Import the new component

@NgModule({
  declarations: [
    AppComponent,
    SummarizerComponent // Declare the new component
  ],
  imports: [
    BrowserModule,
    HttpClientModule, // Add HttpClientModule here
    FormsModule // Add FormsModule if you use ngModel or other template-driven form features
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
