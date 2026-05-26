export const PRICE_UNIT_OPTIONS = [
  { value: "session", label: "Session" },
  { value: "hour", label: "Hour" },
  { value: "project", label: "Project" },
  { value: "package", label: "Package" }
];

export function normalizePriceUnit(value?: string | null) {
  return (
    value
      ?.trim()
      .replace(/^\/+/, "")
      .replace(/^per\s+/i, "")
      .toLowerCase() ?? ""
  );
}

export function formatPriceUnit(value?: string | null) {
  const cleanUnit = normalizePriceUnit(value);

  if (!cleanUnit) {
    return "";
  }

  return (
    PRICE_UNIT_OPTIONS.find((option) => option.value === cleanUnit)
      ?.label ?? cleanUnit
  );
}
