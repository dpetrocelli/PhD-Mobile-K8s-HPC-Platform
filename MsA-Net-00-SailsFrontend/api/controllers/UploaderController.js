const user = 'david';

module.exports = {

    upload: function (req,res){
        sails.log.debug("Vino: ",req.allParams());
        res.send("OK");
    },
    getJob: async function(req,res){
        let params = req.allParams();
        if(params.file){
            let file = params.file.split('.');
            file.pop();
            file = file.join('.');
            let resp = await MariaDB.sendQuery(`SELECT * FROM job WHERE assignedQueue LIKE '${file}%' and state !='finished';`);
            sails.log.verbose(`Get Job ====> ${JSON.stringify(resp)}`);
            res.send(resp);
        }
    },
    getJobByID: async function(req,res){
        let params = req.allParams();
        if(params.id){
            let query = `SELECT * FROM job WHERE id = ${params.id};`
            console.log("GET JOB BY ID ===> ",query)
            let resp = await MariaDB.sendQuery(query);
            sails.log.verbose(`Get Job ====> ${JSON.stringify(resp)}`);
            res.send(resp);
        }
    }
}