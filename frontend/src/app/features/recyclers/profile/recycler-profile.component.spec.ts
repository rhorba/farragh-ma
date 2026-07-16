import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RecyclerProfileComponent } from './recycler-profile.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('RecyclerProfileComponent', () => {
  let component: RecyclerProfileComponent;
  let httpMock: HttpTestingController;
  const zoneUrl = `${environment.apiBaseUrl}/recyclers/zone`;
  const materialsUrl = `${environment.apiBaseUrl}/recyclers/materials`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecyclerProfileComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    const fixture = TestBed.createComponent(RecyclerProfileComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne(zoneUrl).flush({ id: 'z1', centerLatitude: null, centerLongitude: null, radiusM: 5000, polygon: null, createdAt: '2026-01-01T00:00:00Z' });
    httpMock.expectOne(materialsUrl).flush({ materialTypeCodes: [] });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts with the default radius and no error/success message', () => {
    expect(component.form.value.radiusM).toBe(5000);
    expect(component.errorMessage()).toBeNull();
    expect(component.successMessage()).toBeNull();
  });

  it('does not submit an invalid form', () => {
    component.submit();

    expect(component.form.touched).toBe(true);
    expect(component.submitting()).toBe(false);
  });

  it('declares zone and materials on valid submit', () => {
    component.form.setValue({
      centerLatitude: 34.02,
      centerLongitude: -6.83,
      radiusM: 3000,
      materialTypeCodes: ['PLASTIC']
    });

    component.submit();
    expect(component.submitting()).toBe(true);

    httpMock.expectOne(zoneUrl).flush({ id: 'z1', centerLatitude: 34.02, centerLongitude: -6.83, radiusM: 3000, polygon: null, createdAt: '2026-01-01T00:00:00Z' });
    httpMock.expectOne(materialsUrl).flush({ materialTypeCodes: ['PLASTIC'] });

    expect(component.submitting()).toBe(false);
    expect(component.successMessage()).toBe('Profil mis à jour.');
  });

  it('shows an error message when the update fails', () => {
    component.form.setValue({
      centerLatitude: 34.02,
      centerLongitude: -6.83,
      radiusM: 3000,
      materialTypeCodes: ['PLASTIC']
    });

    component.submit();
    httpMock.expectOne(zoneUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });
    httpMock.expectOne(materialsUrl);

    expect(component.submitting()).toBe(false);
    expect(component.errorMessage()).toBe('Erreur lors de la mise à jour du profil. Réessayez.');
  });
});
