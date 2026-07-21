import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminSearchComponent } from './admin-search.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('AdminSearchComponent', () => {
  let component: AdminSearchComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<AdminSearchComponent>>;
  let httpMock: HttpTestingController;
  const usersUrl = `${environment.apiBaseUrl}/admin/users`;
  const requestsUrl = `${environment.apiBaseUrl}/admin/requests`;
  const actionLogUrl = `${environment.apiBaseUrl}/admin/action-log`;

  const sampleUser = { id: 'u1', email: 'test@example.com', role: 'HOUSEHOLD_SME', fullName: 'Test User', phone: null, active: true, createdAt: '2026-01-01T00:00:00Z' };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [AdminSearchComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(AdminSearchComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpMock.expectOne(actionLogUrl).flush({ content: [], totalElements: 0 });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('searches users and renders a row per result', () => {
    component.userFilterForm.setValue({ email: 'test', role: 'HOUSEHOLD_SME' });
    component.searchUsers();

    const req = httpMock.expectOne((r) => r.url === usersUrl);
    expect(req.request.params.get('email')).toBe('test');
    expect(req.request.params.get('role')).toBe('HOUSEHOLD_SME');
    req.flush({
      content: [{ id: 'u1', email: 'test@example.com', role: 'HOUSEHOLD_SME', fullName: 'Test User', phone: null, active: true, createdAt: '2026-01-01T00:00:00Z' }],
      totalElements: 1
    });
    fixture.detectChanges();

    expect(component.users()).toHaveLength(1);
    const rows = fixture.nativeElement.querySelectorAll('.results tbody tr');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('test@example.com');
  });

  it('shows the empty state when no users match', () => {
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [], totalElements: 0 });
    fixture.detectChanges();

    expect(component.users()).toEqual([]);
    const panels = fixture.nativeElement.querySelectorAll('.panel');
    expect(panels[0].querySelector('.empty-state')).toBeTruthy();
  });

  it('does not blow up when the user search fails', () => {
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.usersLoading()).toBe(false);
    expect(component.usersSearched()).toBe(true);
  });

  it('searches requests by status and renders a row per result', () => {
    component.requestFilterForm.setValue({ status: 'POSTED' });
    component.searchRequests();

    const req = httpMock.expectOne((r) => r.url === requestsUrl);
    expect(req.request.params.get('status')).toBe('POSTED');
    req.flush({
      content: [{
        id: 'r1', materialTypeCode: 'PLASTIC', quantityDesc: '5kg', addressText: 'Test address',
        latitude: 34.02, longitude: -6.83, status: 'POSTED', photoUrl: null,
        createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
      }],
      totalElements: 1
    });
    fixture.detectChanges();

    expect(component.requests()).toHaveLength(1);
    const panels = fixture.nativeElement.querySelectorAll('.panel');
    const rows = panels[1].querySelectorAll('.results tbody tr');
    expect(rows).toHaveLength(1);
  });

  it('deactivates an active user and updates the row in place', () => {
    component.userFilterForm.setValue({ email: '', role: '' });
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [sampleUser], totalElements: 1 });
    fixture.detectChanges();

    component.toggleActive(component.users()[0]);
    const req = httpMock.expectOne(`${usersUrl}/u1/deactivate`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...sampleUser, active: false });
    httpMock.expectOne(actionLogUrl).flush({ content: [], totalElements: 0 });
    fixture.detectChanges();

    expect(component.users()[0].active).toBe(false);
    expect(component.togglingUserId()).toBeNull();
  });

  it('reactivates a deactivated user', () => {
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [{ ...sampleUser, active: false }], totalElements: 1 });
    fixture.detectChanges();

    component.toggleActive(component.users()[0]);
    const req = httpMock.expectOne(`${usersUrl}/u1/reactivate`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...sampleUser, active: true });
    httpMock.expectOne(actionLogUrl).flush({ content: [], totalElements: 0 });
    fixture.detectChanges();

    expect(component.users()[0].active).toBe(true);
  });

  it('does not blow up when a deactivate/reactivate call fails', () => {
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [sampleUser], totalElements: 1 });
    fixture.detectChanges();

    component.toggleActive(component.users()[0]);
    httpMock.expectOne(`${usersUrl}/u1/deactivate`).flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.togglingUserId()).toBeNull();
  });

  it('hides the manage button and shows a marker on the signed-in admin\'s own row', () => {
    sessionStorage.setItem('farragh_auth', JSON.stringify({ accessToken: 'token', role: 'ADMIN', userId: 'u1' }));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [AdminSearchComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(AdminSearchComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpMock.expectOne(actionLogUrl).flush({ content: [], totalElements: 0 });

    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [sampleUser], totalElements: 1 });
    fixture.detectChanges();

    const row = fixture.nativeElement.querySelector('.results tbody tr');
    expect(row.querySelector('button')).toBeNull();
    expect(row.textContent).toContain('Vous');
  });

  it('renders the action log after an action is taken', () => {
    component.searchUsers();
    httpMock.expectOne((r) => r.url === usersUrl).flush({ content: [sampleUser], totalElements: 1 });
    fixture.detectChanges();

    component.toggleActive(component.users()[0]);
    httpMock.expectOne(`${usersUrl}/u1/deactivate`).flush({ ...sampleUser, active: false });
    httpMock.expectOne(actionLogUrl).flush({
      content: [{ id: 'log1', adminEmail: 'admin@example.com', targetEmail: 'test@example.com', action: 'DEACTIVATE', createdAt: '2026-01-01T00:00:00Z' }],
      totalElements: 1
    });
    fixture.detectChanges();

    expect(component.actionLog()).toHaveLength(1);
    const panels = fixture.nativeElement.querySelectorAll('.panel');
    const rows = panels[2].querySelectorAll('.results tbody tr');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('admin@example.com');
  });
});
