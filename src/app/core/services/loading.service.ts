import {Injectable, signal} from '@angular/core';
import {toObservable} from '@angular/core/rxjs-interop';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private _loading = signal(false);
  private loadingCount = 0;

  readonly loading = this._loading.asReadonly();
  readonly loading$ = toObservable(this._loading);

  setLoading(loading: boolean): void {
    if (loading) {
      this.loadingCount++;
    } else {
      this.loadingCount = Math.max(0, this.loadingCount - 1);
    }

    this._loading.set(this.loadingCount > 0);
  }

  isLoading(): boolean {
    return this._loading();
  }
}
