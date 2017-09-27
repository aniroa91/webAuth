package model.paytv

import org.joda.time.DateTime



/**
 * contract:BTD018467 object_id:1,013,176,392 name:btdsl-140721-467 profile:ADSL-A1 profile_type:CHI DUNG INTERNET upload_lim:808 download_lim:10,240 
 * status:Da cham dut hop dong mac_address:64:d9:54:b2:a9:90 start_date:1,405,875,600,000 active_date:1,406,020,825,000 change_date:1,501,866,000,000
 *  location:Binh Thuan 
 *  region:Vung 6 point_set:btnp004.20.0134 host:BTNP00403ES52 port:30 slot:4 cable_type:adsl life_time:36 _id:BTD018467 _type:docs _index:internet-contract _score:1
 */
case class InternetContract(
    contract: String,
    objectId: String, 
    name: String,
    profile: String,
    profileType: String,
    updateLim: Long,
    downloadLim: Long,
    status: String,
    macAddress: String,
    start: DateTime,
    active: DateTime,
    change: DateTime,
    province: String,
    region: String,
    point_set: String,
    host: String,
    port: Int,
    slot: Int,
    cable: String,
    life_time: Int)

    /**
     * contract:SGH295964 box_count:1 status:Binh thuong start_date:1,484,983,655,000 active_date:1,485,062,288,000 change_date:1,484,983,655,000
     */
case class PayTVContract(
    contract: String,
    box_count: Int, 
    status: String,
    start: DateTime,
    active: DateTime,
    change: DateTime)
    /**
     * customer_id:374,703 contract:SGD516753 status:Binh thuong change_date:1,441,157,953,000 mac_address:FBOX001c550101bb
     */
case class PayTVBox(
    customer_id: Int,
    contract: String, 
    status: String,
    change: DateTime,
     macAddress: String)


case class ContractResponse()
case class BoxResponse()
     
     