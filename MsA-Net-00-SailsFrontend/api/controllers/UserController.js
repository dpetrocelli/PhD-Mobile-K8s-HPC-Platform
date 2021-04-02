
var request = require('request');
module.exports = {
    home: async function(req,res){
        //var params = req.allParams();
        //var data = await sendRequest(params);
        console.log("/USER ->",req.session);
        res.view('user/home', {
            username: 'david',
            backendUrl: '52.224.128.235:8081'
        });
    },
    createTable: async function(req,res){
        //res.send(`<pre> ${JSON.stringify(await createTableUsers(),null, 2)}</pre>`);
        console.log("Session: ", req.session);
    }



    /**
        res.view('mat_performance/reports', {
            tables: ptables,
            subtitle: 'Reports',
            isAuthorized: authorized,
        });

     */
}

var createTableUsers = async function(){
    let query = `CREATE TABLE users (
                    user varchar(20) NOT NULL,
                    password varchar(30) NOT NULL,
                    isAdmin boolean default false,
                    PRIMARY KEY (user)
                );`
                /**
                 * 
                INSERT INTO users (user, password, isAdmin) VALUES ("admin","admin","true");
                INSERT INTO users (user, password, isAdmin) VALUES ("david","david","false");
                 */
    let users = "select * from users";
    returned = await sendRequest({query:users});
    console.log("--->",returned);

    return returned;
}

var sendRequest = async function(data){
    var respuesta = await MariaDB.sendQuery(data.query);
    console.log("usercontroller->",respuesta);
    return data;
}