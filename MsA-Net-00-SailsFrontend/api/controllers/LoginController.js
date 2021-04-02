module.exports = {
    login: async function(req, res) {
        let params = req.allParams();
        console.log(params);
        // Authentication code here
        if(params.user && params.password){
            var logStatus = await authenticate(params.user, params.password);
            console.log("logstatus -> ",logStatus);
            if(logStatus){
                req.session.user = logStatus;
                req.session.userId = logStatus.user;
                req.session.save();
                if(logStatus.user.isAdmin){
                    return res.json(req.session);
                }else{
                    return res.json(req.session);
                }
            }else{
                return res.send({error:"Invalid user or password"});
            }
        }
      }
}

var authenticate = async function(user, pass){
    var query = `SELECT * FROM users WHERE (users.user = "${sanitizeString(user)}" AND users.password = "${sanitizeString(pass)}")`;
    return new Promise(async (resolve,reject)=>{
        let r = await MariaDB.sendQuery(query);
        console.log("Login -> auth -> response:", JSON.stringify(r,null,2));
        if( r[0]) resolve(r[0]);
        else reject(r);
    })
}
function sanitizeString(str){
    str = str.replace(/([^a-z0-9áéíóúñü_-\s\.,]|[\t\n\f\r\v\0])/gim,"");
    return str.trim();
}
/**
 *   login: function(req, res) {

    // Authentication code here

    // If successfully authenticated

    req.session.userId = foundUser.id;   // returned from a database

    return res.json(foundUser);

  }
 */