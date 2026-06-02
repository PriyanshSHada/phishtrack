const pad = (n, width = 3) => n.toString().padStart(width, '0');

exports.generateCaseNumber = (seq, date = new Date()) => {
  const year = date.getFullYear();
  return `CASE-${year}-${pad(seq, 3)}`;
};
