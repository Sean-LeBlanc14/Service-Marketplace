// Shim for react-aria/SSRProvider and react-aria/private/ssr/SSRProvider
// These modules were removed in react-aria@3.48.0 but @react-aria/ssr@3.10.0
// still imports them for backward-compatibility re-exports.

export function SSRProvider({ children }) {
  return children;
}

export function useIsSSR() {
  return false;
}

export function useSSRSafeId(defaultId) {
  return defaultId ?? Math.random().toString(36).slice(2);
}
