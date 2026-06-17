exports.getVersion = async (req, res, next) => {
  try {
    // In a real application, you might fetch this from a database table 
    // or environment variables. For now, we return a hardcoded minimum 
    // version required by the app to function properly.
    res.json({
      minVersion: "1.0.0",
      latestVersion: "1.0.0"
    });
  } catch (err) {
    next(err);
  }
};
