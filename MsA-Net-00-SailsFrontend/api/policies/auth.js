
module.exports = function(req, res, next) {
    console.log("Session: ",req.originalUrl)
    if(req.originalUrl == '/log' && req.originalUrl == '/login'){
        return next();
    }
    // User is allowed, proceed to the next policy,
    console.log("Session is: ",req.session)
    if (req.session.user) {
        return next();
    }
  
    // User is not allowed
    return next(); // arreglar
    return res.redirect('/log');
};