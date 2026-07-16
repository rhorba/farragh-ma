import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RecyclerFeedComponent } from './recycler-feed.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('RecyclerFeedComponent', () => {
  let component: RecyclerFeedComponent;
  let httpMock: HttpTestingController;
  const feedUrl = `${environment.apiBaseUrl}/recyclers/feed`;

  const sampleRequest = {
    id: 'r1',
    materialTypeCode: 'PLASTIC',
    quantityDesc: '5kg',
    addressText: 'Test address',
    latitude: 34.02,
    longitude: -6.83,
    status: 'POSTED',
    photoUrl: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecyclerFeedComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    const fixture = TestBed.createComponent(RecyclerFeedComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the matched feed on init', () => {
    httpMock.expectOne(feedUrl).flush([sampleRequest]);

    expect(component.loading()).toBe(false);
    expect(component.loadError()).toBe(false);
    expect(component.requests()).toEqual([sampleRequest]);
  });

  it('sets loadError when the feed fails to load', () => {
    httpMock.expectOne(feedUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.loading()).toBe(false);
    expect(component.loadError()).toBe(true);
  });

  it('removes the request from the list on successful accept', () => {
    httpMock.expectOne(feedUrl).flush([sampleRequest]);

    component.accept('r1');
    httpMock.expectOne(`${feedUrl}/r1/accept`).flush(sampleRequest);

    expect(component.requests()).toEqual([]);
    expect(component.acceptingId()).toBeNull();
  });

  it('shows a conflict message and reloads the feed when accept loses the race', () => {
    httpMock.expectOne(feedUrl).flush([sampleRequest]);

    component.accept('r1');
    httpMock.expectOne(`${feedUrl}/r1/accept`).flush('conflict', { status: 409, statusText: 'Conflict' });
    httpMock.expectOne(feedUrl).flush([]);

    expect(component.acceptError()).toBe('Cette demande vient d\'être acceptée par un autre recycleur.');
    expect(component.acceptingId()).toBeNull();
  });
});
