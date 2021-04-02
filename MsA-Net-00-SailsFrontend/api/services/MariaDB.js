const mariadb = require('mariadb');
/**
 * 
    [13:45, 24/5/2020] David Petrocelli: mariadb:52.224.200.71
[13:45, 24/5/2020] David Petrocelli: user: david ; pwd: david
[13:45, 24/5/2020] David Petrocelli: base distribuitedProcessing
 */

const pool = mariadb.createPool({
     host: '52.191.102.99', 
     user:'david', 
     password: 'david',
     connectTimeout: '40000',
     database: 'distributedProcessing',
     connectionLimit: 5
});

module.exports = {
    sendQuery: function(query){
        return new Promise(async (resolve,reject)=>{
            let conn;
            try {
                conn = await pool.getConnection();
                if(query){
                    var a = await conn.query(query);
                    resolve(a);
                }else{
                    return null;
                }
            } catch (err) {
                console.log("ERROR: ",err);
                reject(err);
            } finally {
                if (conn) return conn.end();
            }
        })
    } 

}

